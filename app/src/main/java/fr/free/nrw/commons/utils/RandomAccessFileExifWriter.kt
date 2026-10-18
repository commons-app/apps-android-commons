package fr.free.nrw.commons.utils

import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.iterator

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

    private const val TYPE_BYTE = 1
    private const val TYPE_ASCII = 2
    private const val TYPE_SHORT = 3
    private const val TYPE_LONG = 4
    private const val TYPE_RATIONAL = 5
    private const val TYPE_UNDEFINED = 7
    private const val TYPE_SLONG = 9
    private const val TYPE_SRATIONAL = 10

    private enum class IfdGroup { IFD0, EXIF, GPS }

    /** Specification for one tag: numeric ID, EXIF type, owning IFD. */
    private data class TagSpec(val id: Int, val type: Int, val ifd: IfdGroup)

    // Map ExifInterface tag-name constants (ID, data-type, IFD group).
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

    /** GPS IFD pointer tag – lives in IFD0, points to the GPS sub-IFD. */
    private const val TAG_ID_GPS_IFD_POINTER = 0x8825

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
        if (!file.exists() || file.length() < 4 || tags.isEmpty()) return

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
        if (!file.exists() || file.length() < 4) return

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
        if (!file.exists() || file.length() < 4 || redactTags.isEmpty()) return

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
                if (raf.readUnsignedShort() != 0xFFD8) return

                while (raf.filePointer < raf.length() - 4) {
                    val marker = raf.readUnsignedShort()
                    val len = raf.readUnsignedShort()
                    val next = raf.filePointer + len - 2

                    if (marker == 0xFFE1 && len >= 8) {
                        val app1Start = raf.filePointer
                        val hdr = ByteArray(6)
                        raf.readFully(hdr)
                        if (String(hdr, 0, 4) == "Exif") {
                            val tiffBase = app1Start + 6
                            val bo = ByteArray(2)
                            raf.readFully(bo)
                            val order = if (bo[0] == 'I'.code.toByte())
                                ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

                            readU16(raf, order) // skip magic 42
                            val ifd0Off = readU32(raf, order)
                            scanAndUpdateIfd(
                                raf, tiffBase, tiffBase + ifd0Off,
                                order, updates,
                            )
                            break
                        } else {
                            raf.seek(next)
                        }
                    } else if (marker == 0xFFDA) break
                    else raf.seek(next)
                }
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

        for (i in 0 until count) {
            val entryPos = raf.filePointer
            val tagId = readU16(raf, order)
            val type = readU16(raf, order)
            val cnt = readU32(raf, order)
            val valFieldPos = raf.filePointer
            val valOrOff = readU32(raf, order)
            val afterEntry = raf.filePointer

            // Recurse sub-IFDs.
            if ((tagId == 0x8769 || tagId == 0x8825) && valOrOff > 0) {
                scanAndUpdateIfd(
                    raf, tiffBase, tiffBase + valOrOff, order, updates, isMainIfd = false,
                )
                raf.seek(afterEntry)
            }

            // Remove or overwrite matching tags.
            if (!updates.containsKey(tagId)) continue

            if (updates[tagId] == null) {
                // Zero out the entire 12-byte IFD entry to remove the tag
                raf.seek(entryPos)
                raf.write(ByteArray(12))
                raf.seek(afterEntry)
            } else {
                val encoded = encodeValue(type, updates[tagId]!!, order) ?: continue
                val unitSize = typeUnitSize(type)
                val existingBytes = unitSize * cnt

                if (existingBytes <= 4) {
                    if (encoded.size <= 4) {
                        raf.seek(valFieldPos)
                        raf.write(encoded)
                        repeat(4 - encoded.size) { raf.writeByte(0) }
                    }
                } else {
                    if (encoded.size <= existingBytes) {
                        raf.seek(tiffBase + valOrOff)
                        raf.write(encoded)
                        repeat(existingBytes - encoded.size) { raf.writeByte(0) }
                    } else {
                        Timber.w(
                            "Tag 0x%04X: new value (%d B) exceeds existing space (%d B), skipped",
                            tagId, encoded.size, existingBytes,
                        )
                    }
                }
                raf.seek(afterEntry)
            }
        }

        // Only main IFDs (e.g. IFD0 -> IFD1 thumbnail IFD) have a next-IFD offset pointer.
        if (isMainIfd && raf.filePointer <= raf.length() - 4) {
            val nextIfdOff = readU32(raf, order)
            if (nextIfdOff > 0) {
                scanAndUpdateIfd(
                    raf, tiffBase, tiffBase + nextIfdOff, order, updates, isMainIfd = true,
                )
            }
        }
    }

    /**
     * Encodes a string value into a byte array matching the given EXIF
     * [type], using [order] for multi-byte integers.
     */
    private fun encodeValue(type: Int, value: String, order: ByteOrder): ByteArray? {
        return try {
            when (type) {
                TYPE_BYTE ->
                    byteArrayOf(value.toInt().toByte())

                TYPE_ASCII ->
                    "$value\u0000".toByteArray(Charsets.US_ASCII)

                TYPE_SHORT ->
                    ByteBuffer.allocate(2).order(order)
                        .putShort(value.toInt().toShort()).array()

                TYPE_LONG ->
                    ByteBuffer.allocate(4).order(order)
                        .putInt(value.toLong().toInt()).array()

                TYPE_RATIONAL, TYPE_SRATIONAL -> {
                    val parts = value.split(",")
                    val buf = ByteBuffer.allocate(parts.size * 8).order(order)
                    for (part in parts) {
                        val nd = part.trim().split("/")
                        if (nd.size != 2) return null
                        buf.putInt(nd[0].trim().toInt())
                        buf.putInt(nd[1].trim().toInt())
                    }
                    buf.array()
                }

                TYPE_SLONG ->
                    ByteBuffer.allocate(4).order(order)
                        .putInt(value.toInt()).array()

                TYPE_UNDEFINED ->
                    value.toByteArray(Charsets.US_ASCII)

                else -> null
            }
        } catch (e: NumberFormatException) {
            Timber.w("Cannot encode value '%s' for EXIF type %d", value, type)
            null
        }
    }

    /** Size in bytes of one unit of the given EXIF [type]. */
    private fun typeUnitSize(type: Int): Int = when (type) {
        TYPE_BYTE, TYPE_ASCII, TYPE_UNDEFINED -> 1
        TYPE_SHORT -> 2
        TYPE_LONG, TYPE_SLONG -> 4
        TYPE_RATIONAL, TYPE_SRATIONAL -> 8
        else -> 1
    }

    private fun readU16(raf: RandomAccessFile, order: ByteOrder): Int {
        val b1 = raf.readUnsignedByte()
        val b2 = raf.readUnsignedByte()
        return if (order == ByteOrder.LITTLE_ENDIAN) (b2 shl 8) or b1
        else (b1 shl 8) or b2
    }

    private fun readU32(raf: RandomAccessFile, order: ByteOrder): Int {
        val b1 = raf.readUnsignedByte()
        val b2 = raf.readUnsignedByte()
        val b3 = raf.readUnsignedByte()
        val b4 = raf.readUnsignedByte()
        return if (order == ByteOrder.LITTLE_ENDIAN)
            (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
        else
            (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
    }
}