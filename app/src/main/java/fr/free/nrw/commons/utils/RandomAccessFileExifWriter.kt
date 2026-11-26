package fr.free.nrw.commons.utils

import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.utils.ExifValueEncoder.LONG_SIZE_BYTES
import fr.free.nrw.commons.utils.ExifValueEncoder.TYPE_ASCII
import fr.free.nrw.commons.utils.ExifValueEncoder.TYPE_BYTE
import fr.free.nrw.commons.utils.ExifValueEncoder.TYPE_LONG
import fr.free.nrw.commons.utils.ExifValueEncoder.TYPE_RATIONAL
import fr.free.nrw.commons.utils.ExifValueEncoder.TYPE_SHORT
import fr.free.nrw.commons.utils.ExifValueEncoder.encodeValue
import fr.free.nrw.commons.utils.ExifValueEncoder.readU16
import fr.free.nrw.commons.utils.ExifValueEncoder.readU32
import fr.free.nrw.commons.utils.ExifValueEncoder.typeUnitSize
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteOrder
import kotlin.collections.iterator

private const val JPEG_MARKER_SOI = 0xFFD8
private const val JPEG_MARKER_APP1 = 0xFFE1
private const val JPEG_MARKER_SOS = 0xFFDA
private const val MIN_FILE_SIZE = 4L
private const val APP1_MIN_LENGTH = 8
private const val EXIF_HEADER_LENGTH = 6
private const val EXIF_SIGNATURE_LENGTH = 4

private data class ExifLocation(val tiffBase: Long, val ifd0Offset: Int, val order: ByteOrder)

/**
 * General-purpose EXIF metadata writer that operates on JPEG files via [RandomAccessFile],
 * avoiding [ExifInterface.saveAttributes].
 *
 * ## Core API
 * - [writeTag]  – update or remove a single tag in-place.
 * - [writeTags] – update or remove multiple tags in-place.
 *
 * ## Convenience helpers
 * - [removeLocation] – strips all GPS tags.
 * - [redactTags]     – strips tags by ExifInterface tag name.
 * - [copyExif]       – copies EXIF from an [androidx.exifinterface.media.ExifInterface] and writes into a file.
 */
object RandomAccessFileExifWriter {

    private enum class IfdGroup { IFD0, EXIF, GPS }

    /** Specification for one tag: numeric ID, EXIF type, owning IFD. */
    private data class TagSpec(val id: Int, val type: Int, val ifd: IfdGroup)

    // Map ExifInterface tag-name constants (ID, data-type, IFD group).
    // Tag IDs are defined by the standard EXIF 2.32 / TIFF specifications
    @Suppress("MagicNumber") // tag IDs are defined by the EXIF standard
    private val TAG_REGISTRY: Map<String, TagSpec> = mapOf(

        // IFD0 tags 3.
        ExifInterface.TAG_IMAGE_DESCRIPTION to TagSpec(0x010E, TYPE_ASCII, IfdGroup.IFD0),
        ExifInterface.TAG_MAKE to TagSpec(0x010F, TYPE_ASCII, IfdGroup.IFD0),
        ExifInterface.TAG_MODEL to TagSpec(0x0110, TYPE_ASCII, IfdGroup.IFD0),
        ExifInterface.TAG_ORIENTATION to TagSpec(0x0112, TYPE_SHORT, IfdGroup.IFD0),
        ExifInterface.TAG_SOFTWARE to TagSpec(0x0131, TYPE_ASCII, IfdGroup.IFD0),
        ExifInterface.TAG_DATETIME to TagSpec(0x0132, TYPE_ASCII, IfdGroup.IFD0),
        ExifInterface.TAG_ARTIST to TagSpec(0x013B, TYPE_ASCII, IfdGroup.IFD0),
        ExifInterface.TAG_COPYRIGHT to TagSpec(0x8298, TYPE_ASCII, IfdGroup.IFD0),

        // Exif sub-IFD tags.
        ExifInterface.TAG_EXPOSURE_TIME to TagSpec(0x829A, TYPE_RATIONAL, IfdGroup.EXIF),
        ExifInterface.TAG_F_NUMBER to TagSpec(0x829D, TYPE_RATIONAL, IfdGroup.EXIF),
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY to TagSpec(0x8827, TYPE_SHORT, IfdGroup.EXIF),
        ExifInterface.TAG_DATETIME_ORIGINAL to TagSpec(0x9003, TYPE_ASCII, IfdGroup.EXIF),
        ExifInterface.TAG_DATETIME_DIGITIZED to TagSpec(0x9004, TYPE_ASCII, IfdGroup.EXIF),
        ExifInterface.TAG_FLASH to TagSpec(0x9209, TYPE_SHORT, IfdGroup.EXIF),
        ExifInterface.TAG_FOCAL_LENGTH to TagSpec(0x920A, TYPE_RATIONAL, IfdGroup.EXIF),
        ExifInterface.TAG_IMAGE_WIDTH to TagSpec(0xA002, TYPE_LONG, IfdGroup.EXIF),
        ExifInterface.TAG_IMAGE_LENGTH to TagSpec(0xA003, TYPE_LONG, IfdGroup.EXIF),
        ExifInterface.TAG_CAMERA_OWNER_NAME to TagSpec(0xA430, TYPE_ASCII, IfdGroup.EXIF),
        ExifInterface.TAG_BODY_SERIAL_NUMBER to TagSpec(0xA431, TYPE_ASCII, IfdGroup.EXIF),
        ExifInterface.TAG_LENS_SPECIFICATION to TagSpec(0xA432, TYPE_RATIONAL, IfdGroup.EXIF),
        ExifInterface.TAG_LENS_MAKE to TagSpec(0xA433, TYPE_ASCII, IfdGroup.EXIF),
        ExifInterface.TAG_LENS_MODEL to TagSpec(0xA434, TYPE_ASCII, IfdGroup.EXIF),
        ExifInterface.TAG_LENS_SERIAL_NUMBER to TagSpec(0xA435, TYPE_ASCII, IfdGroup.EXIF),
        ExifInterface.TAG_WHITE_BALANCE to TagSpec(0xA405, TYPE_SHORT, IfdGroup.EXIF),

        // GPS sub-IFD tags.
        ExifInterface.TAG_GPS_LATITUDE_REF to TagSpec(0x0001, TYPE_ASCII, IfdGroup.GPS),
        ExifInterface.TAG_GPS_LATITUDE to TagSpec(0x0002, TYPE_RATIONAL, IfdGroup.GPS),
        ExifInterface.TAG_GPS_LONGITUDE_REF to TagSpec(0x0003, TYPE_ASCII, IfdGroup.GPS),
        ExifInterface.TAG_GPS_LONGITUDE to TagSpec(0x0004, TYPE_RATIONAL, IfdGroup.GPS),
        ExifInterface.TAG_GPS_ALTITUDE_REF to TagSpec(0x0005, TYPE_BYTE, IfdGroup.GPS),
        ExifInterface.TAG_GPS_ALTITUDE to TagSpec(0x0006, TYPE_RATIONAL, IfdGroup.GPS),
        ExifInterface.TAG_GPS_TIMESTAMP to TagSpec(0x0007, TYPE_RATIONAL, IfdGroup.GPS),
        ExifInterface.TAG_GPS_DATESTAMP to TagSpec(0x001D, TYPE_ASCII, IfdGroup.GPS),
    )

    /** Special IFD pointer tags. */
    private const val TAG_ID_EXIF_IFD_POINTER = 0x8769
    private const val TAG_ID_GPS_IFD_POINTER = 0x8825

    /** EXIF / TIFF binary format specifications. */
    private const val IFD_ENTRY_SIZE_BYTES = 12 // Each TIFF IFD directory entry is 12 bytes
    private const val IFD_VALUE_OR_OFFSET_SIZE_BYTES = 4 // Values > 4 bytes are stored as offsets

    /**
     * Encapsulates parameters for updating an EXIF tag entry.
     *
     * @param tiffBase Base offset of the TIFF structure in the file
     * @param type EXIF data type (e.g., TYPE_ASCII, TYPE_SHORT, TYPE_RATIONAL)
     * @param cnt Count of values for this tag
     * @param valFieldPos File position of the value/offset field in the IFD entry
     * @param valOrOff Value or offset read from the IFD entry
     * @param afterEntry File position immediately after this IFD entry
     * @param tagId Numeric tag identifier
     * @param value New string value to write
     * @param order Byte order (little-endian or big-endian)
     */
    private data class TagUpdateParams(
        val tiffBase: Long,
        val type: Int,
        val cnt: Int,
        val valFieldPos: Long,
        val valOrOff: Int,
        val afterEntry: Long,
        val tagId: Int,
        val value: String,
        val order: ByteOrder,
    )

    /**
     * Update a single EXIF tag in a JPEG file.
     *
     * @param file    target JPEG file.
     * @param tagName an [ExifInterface] tag constant, e.g. [ExifInterface.TAG_ORIENTATION]
     * @param value   new value as a string (same format [ExifInterface.getAttribute]
     *                returns), or **null** to remove the tag
     */
    fun writeTag(file: File, tagName: String, value: String?) {
        writeTags(file, mapOf(tagName to value))
    }

    /**
     * Update (or remove) multiple EXIF tags in a JPEG file in a single pass.
     *
     * @param tags map of tag-name → value (null = remove)
     */
    fun writeTags(file: File, tags: Map<String, String?>) {
        if (!file.exists() || file.length() < MIN_FILE_SIZE || tags.isEmpty()) return

        val updates = mutableMapOf<Int, String?>()
        for ((name, value) in tags) {
            val spec = TAG_REGISTRY[name] ?: continue
            val targetValue = if (name == ExifInterface.TAG_ORIENTATION && value == null) "1" else value
            updates[spec.id] = targetValue
            if (value == null && spec.ifd == IfdGroup.GPS) {
                updates[TAG_ID_GPS_IFD_POINTER] = null
            }
        }
        if (updates.isEmpty()) return

        writeTagsInternal(file, updates)
    }

    /**
     * Strips all GPS location metadata from a JPEG file.
     */
    fun removeLocation(file: File) {
        if (!file.exists() || file.length() < MIN_FILE_SIZE) return

        val updates = TAG_REGISTRY
            .filter { it.value.ifd == IfdGroup.GPS }
            .map { it.value.id }
            .plus(TAG_ID_GPS_IFD_POINTER)
            .associateWith { null as String? }

        writeTagsInternal(file, updates)
    }

    /**
     * Redacts (nulls) tags identified by their [ExifInterface] tag-name
     * constants (e.g. `ExifInterface.TAG_MAKE`).
     */
    fun redactTags(file: File, redactTags: Set<String>) {
        if (!file.exists() || file.length() < MIN_FILE_SIZE || redactTags.isEmpty()) return

        val updates = mutableMapOf<Int, String?>()
        for (name in redactTags) {
            val spec = TAG_REGISTRY[name] ?: continue
            val targetValue = if (name == ExifInterface.TAG_ORIENTATION) "1" else null
            updates[spec.id] = targetValue
            if (spec.ifd == IfdGroup.GPS) updates[TAG_ID_GPS_IFD_POINTER] = null
        }
        if (updates.isEmpty()) return

        writeTagsInternal(file, updates)
    }

    /**
     * Copies all supported EXIF metadata from [src] Exif into [dstFile]
     * by reading each known tag and writing it via [writeTags].
     */
    fun copyExif(src: ExifInterface, dstFile: File) {
        val tags = mutableMapOf<String, String?>()
        for ((tagName, _) in TAG_REGISTRY) {
            val value = src.getAttribute(tagName) ?: continue
            tags[tagName] = value
        }
        if (tags.isNotEmpty()) {
            writeTags(dstFile, tags)
        }
    }

    /**
     * Opens the file, locates the APP1 segment, and applies [updates] through [scanAndUpdateIfd]
     * (tag-ID → new value / null) in-place.
     */
    private fun writeTagsInternal(
        file: File,
        updates: Map<Int, String?>,
    ) {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                if (raf.readUnsignedShort() != JPEG_MARKER_SOI) return

                val exif = App1SegmentProcessor.findExif(raf) ?: return
                scanAndUpdateIfd(
                    raf, exif.tiffBase, exif.tiffBase + exif.ifd0Offset,
                    exif.order, updates,
                )
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to update EXIF tags via RandomAccessFile")
        }
    }

    /**
     * Walks every entry in one IFD, applying matching updates or zeroing
     * entries. Only recurses into Exif sub-IFD (0x8769) / GPS sub-IFD
     * (0x8825) when the pointer tag is encountered.
     */
    private fun scanAndUpdateIfd(
        raf: RandomAccessFile,
        tiffBase: Long,
        ifdPos: Long,
        order: ByteOrder,
        updates: Map<Int, String?>,
        isMainIfd: Boolean = true,
    ) {
        if (ifdPos >= raf.length() - 2) return
        raf.seek(ifdPos)
        val count = readU16(raf, order)

        repeat(count) {
            processIfdEntry(raf, tiffBase, order, updates)
        }

        // Only main IFDs (e.g. IFD0 -> IFD1 thumbnail IFD) have a next-IFD offset pointer.
        if (isMainIfd && raf.filePointer <= raf.length() - LONG_SIZE_BYTES) {
            val nextIfdOff = readU32(raf, order)
            if (nextIfdOff > 0) {
                scanAndUpdateIfd(
                    raf, tiffBase, tiffBase + nextIfdOff, order, updates, isMainIfd = true,
                )
            }
        }
    }
    /**
     * Processes a single IFD entry, handling sub-IFD recursion and tag updates.
     */
    private fun processIfdEntry(
        raf: RandomAccessFile,
        tiffBase: Long,
        order: ByteOrder,
        updates: Map<Int, String?>,
    ) {
        val entryPos = raf.filePointer
        val tagId = readU16(raf, order)
        val type = readU16(raf, order)
        val cnt = readU32(raf, order)
        val valFieldPos = raf.filePointer
        val valOrOff = readU32(raf, order)
        val afterEntry = raf.filePointer

        // Recurse sub-IFDs.
        if ((tagId == TAG_ID_EXIF_IFD_POINTER || tagId == TAG_ID_GPS_IFD_POINTER) && valOrOff > 0) {
            scanAndUpdateIfd(
                raf, tiffBase, tiffBase + valOrOff, order, updates, isMainIfd = false,
            )
            raf.seek(afterEntry)
        }

        // Remove or overwrite matching tags.
        if (!updates.containsKey(tagId)) return

        if (updates[tagId] == null) {
            removeTag(raf, entryPos, afterEntry)
        } else {
            updateTag(
                raf,
                TagUpdateParams(
                    tiffBase = tiffBase,
                    type = type,
                    cnt = cnt,
                    valFieldPos = valFieldPos,
                    valOrOff = valOrOff,
                    afterEntry = afterEntry,
                    tagId = tagId,
                    value = updates[tagId]!!,
                    order = order,
                ),
            )
        }
    }

    /**
     * Removes a tag by zeroing out its IFD entry.
     */
    private fun removeTag(raf: RandomAccessFile, entryPos: Long, afterEntry: Long) {
        raf.seek(entryPos)
        raf.write(ByteArray(IFD_ENTRY_SIZE_BYTES))
        raf.seek(afterEntry)
    }

    /**
     * Updates a tag with a new encoded value.
     */
    private fun updateTag(
        raf: RandomAccessFile,
        params: TagUpdateParams,
    ) {
        val encoded = encodeValue(params.type, params.value, params.order) ?: return
        val unitSize = typeUnitSize(params.type)
        val existingBytes = unitSize * params.cnt

        if (existingBytes <= IFD_VALUE_OR_OFFSET_SIZE_BYTES) {
            if (encoded.size <= IFD_VALUE_OR_OFFSET_SIZE_BYTES) {
                raf.seek(params.valFieldPos)
                raf.write(encoded)
                repeat(IFD_VALUE_OR_OFFSET_SIZE_BYTES - encoded.size) { raf.writeByte(0) }
            }
        } else {
            if (encoded.size <= existingBytes) {
                raf.seek(params.tiffBase + params.valOrOff)
                raf.write(encoded)
                repeat(existingBytes - encoded.size) { raf.writeByte(0) }
            } else {
                Timber.w(
                    "Tag 0x%04X: new value (%d B) exceeds existing space (%d B), skipped",
                    params.tagId, encoded.size, existingBytes,
                )
            }
        }
        raf.seek(params.afterEntry)
    }

}

private object App1SegmentProcessor {

    /**
     * Finds the APP1/EXIF segment in the JPEG data, or null if there is none.
     */
    fun findExif(raf: RandomAccessFile): ExifLocation? {
        var location: ExifLocation? = null
        while (location == null && raf.filePointer < raf.length() - MIN_FILE_SIZE) {
            val marker = raf.readUnsignedShort()
            val len = raf.readUnsignedShort()
            val next = raf.filePointer + len - 2

            when {
                marker == JPEG_MARKER_APP1 && len >= APP1_MIN_LENGTH ->
                    location = readExifLocation(raf, next)
                marker == JPEG_MARKER_SOS -> break
                else -> raf.seek(next)
            }
        }
        return location
    }

    /**
     * Reads the EXIF header and byte order of the current APP1 segment.
     */
    private fun readExifLocation(raf: RandomAccessFile, next: Long): ExifLocation? {
        val app1Start = raf.filePointer
        val hdr = ByteArray(EXIF_HEADER_LENGTH)
        raf.readFully(hdr)

        if (String(hdr, 0, EXIF_SIGNATURE_LENGTH) != "Exif") {
            raf.seek(next)
            return null
        }

        val tiffBase = app1Start + EXIF_HEADER_LENGTH
        val bo = ByteArray(2)
        raf.readFully(bo)
        val order = if (bo[0] == 'I'.code.toByte())
            ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

        readU16(raf, order) // skip magic 42
        return ExifLocation(tiffBase, readU32(raf, order), order)
    }
}
