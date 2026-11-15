package fr.free.nrw.commons.utils

import java.io.File
import java.io.RandomAccessFile

/**
 * Removes the short video that phones append to JPEG motion photos.
 * Format: https://developer.android.com/media/platform/motion-photo-format
 */
object MotionPhotoUtils {
    private const val JPEG_SOI = 0xFFD8
    private const val MARKER_PREFIX = 0xFF
    private const val MARKER_APP1 = 0xE1
    private const val MARKER_SOS = 0xDA
    private const val SEGMENT_LENGTH_BYTES = 2
    private const val SEGMENT_HEADER_BYTES = 4
    private const val XMP_HEADER = "http://ns.adobe.com/xap/1.0/\u0000"

    // An MP4 starts with its "ftyp" box: 4 bytes of size, then "ftyp".
    private const val FTYP_OFFSET = 4
    private const val FTYP = "ftyp"
    private const val VIDEO_SEMANTIC = "Item:Semantic=\"MotionPhoto\""

    // Samsung puts this 24-byte header right before the video: two zero bytes, the entry type
    // 0x0A30 and the name length 16 as little-endian numbers, then the name. For example:
    // https://commons.wikimedia.org/wiki/File:20230228_131217_February_2023_in_Ramat_Gan.jpg
    // https://commons.wikimedia.org/wiki/File:20230301_092731_March_2023_in_Ramat_Gan.jpg
    private const val SAMSUNG_VIDEO_HEADER = "\u0000\u0000\u0030\u000A\u0010\u0000\u0000\u0000MotionPhoto_Data"
    private val samsungVideoHeader = SAMSUNG_VIDEO_HEADER.toByteArray(Charsets.ISO_8859_1)

    // shortcut: only the attribute form Google cameras write, not other XMP prefixes or element form.
    private val motionPhotoFlag = Regex("""GCamera:MotionPhoto="1"""")

    // An empty item, self-closing or as an open/close pair.
    private const val ITEM = """<Container:Item\b[^>]*(?:/>|>\s*</Container:Item>)"""

    // The item with its <rdf:li> wrapper when it has one, so the whole entry can be blanked out.
    // Group 1 is an item with children: only its opening tag is matched, so it can't be blanked.
    private val containerItem =
        Regex("""<rdf:li\b[^>]*>\s*$ITEM\s*</rdf:li>|$ITEM|(<Container:Item\b[^>]*>)""")
    private val videoLength = Regex("""Item:Length="(\d+)"""")

    // The older "MicroVideo" format, written for example by the Samsung Galaxy S9 (see the pictures above).
    // The offset counts the bytes from the start of the video to the end of the file.
    private val microVideoFlag = Regex("""GCamera:MicroVideo="1"""")
    private val microVideoOffset = Regex("""GCamera:MicroVideoOffset="(\d+)"""")

    private class Video(
        val start: Long,
        val flagOffset: Long,
        val itemOffset: Long = 0,
        val itemLength: Int = 0,
    )

    /**
     * If [file] is a motion photo whose video sits at the end of the file, as the format requires,
     * cuts the video off, along with the Samsung header right before it if there is one,
     * and marks the photo as a still image.
     * Anything that does not match is left untouched.
     *
     * @return true if a video was removed
     */
    fun removeVideo(file: File): Boolean =
        RandomAccessFile(file, "rw").use { raf ->
            val video = findVideo(raf)
            if (video != null) {
                raf.setLength(video.start)
                // The value is a single ASCII byte, so it can be changed in place without moving anything.
                raf.seek(video.flagOffset)
                raf.write('0'.code)
                // Spaces keep every offset and the segment length valid, and are allowed between XMP elements.
                raf.seek(video.itemOffset)
                raf.write(ByteArray(video.itemLength) { ' '.code.toByte() })
            }
            video != null
        }

    private fun findVideo(raf: RandomAccessFile): Video? {
        val (xmpStart, xmp) = findXmp(raf) ?: return null
        // Read the attributes from the Container:Item tag only, not from its rdf:li wrapper.
        val item = containerItem.findAll(xmp).firstOrNull { itemTag(it).contains(VIDEO_SEMANTIC) }
        val blank = item?.takeIf { it.groups[1] == null }?.range
        val motionPhotoLength =
            item
                ?.let { videoLength.find(itemTag(it)) }
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
        val microVideoLength =
            microVideoOffset
                .find(xmp)
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
        // MicroVideo has no Container:Item, so only a MotionPhoto video has an entry to blank.
        return videoAtEnd(raf, xmpStart, xmp, motionPhotoFlag.find(xmp), motionPhotoLength)
            ?.let { Video(it.start, it.flagOffset, xmpStart + (blank?.first ?: 0), blank?.count() ?: 0) }
            ?: videoAtEnd(raf, xmpStart, xmp, microVideoFlag.find(xmp), microVideoLength)
    }

    // Every match of containerItem contains this tag, with or without the wrapper.
    private fun itemTag(match: MatchResult) = match.value.substring(match.value.indexOf("<Container:Item"))

    /**
     * Checks that a video of [length] bytes ends the file, after the XMP packet.
     */
    private fun videoAtEnd(
        raf: RandomAccessFile,
        xmpStart: Long,
        xmp: String,
        flag: MatchResult?,
        length: Long?,
    ): Video? {
        val xmpEnd = xmpStart + xmp.length
        val start =
            length
                ?.takeIf { it > FTYP_OFFSET + FTYP.length }
                ?.let { raf.length() - it }
                ?.takeIf { it > xmpEnd && hasFtypAt(raf, it) }
        return if (flag != null && start != null) {
            Video(withSamsungHeader(raf, start, xmpEnd), xmpStart + flag.range.last - 1)
        } else {
            null
        }
    }

    /**
     * @return the start of the Samsung header if it sits right before [videoStart], else [videoStart]
     */
    private fun withSamsungHeader(
        raf: RandomAccessFile,
        videoStart: Long,
        xmpEnd: Long,
    ): Long {
        val headerStart = videoStart - samsungVideoHeader.size
        if (headerStart <= xmpEnd) return videoStart
        val bytes = ByteArray(samsungVideoHeader.size)
        raf.seek(headerStart)
        raf.readFully(bytes)
        return if (bytes.contentEquals(samsungVideoHeader)) headerStart else videoStart
    }

    private fun hasFtypAt(
        raf: RandomAccessFile,
        start: Long,
    ): Boolean {
        val box = ByteArray(FTYP.length)
        raf.seek(start + FTYP_OFFSET)
        raf.readFully(box)
        return String(box, Charsets.US_ASCII) == FTYP
    }

    /**
     * Finds the main XMP packet among the segments before the image data.
     *
     * @return the offset of the packet in the file and its text, one char per byte
     */
    private fun findXmp(raf: RandomAccessFile): Pair<Long, String>? {
        var xmp: Pair<Long, String>? = null
        raf.seek(0)
        var inHeader = raf.length() > SEGMENT_HEADER_BYTES && raf.readUnsignedShort() == JPEG_SOI
        while (inHeader && xmp == null && raf.filePointer + SEGMENT_HEADER_BYTES <= raf.length()) {
            val prefix = raf.readUnsignedByte()
            val marker = raf.readUnsignedByte()
            val dataLength = raf.readUnsignedShort() - SEGMENT_LENGTH_BYTES
            val dataStart = raf.filePointer
            inHeader = prefix == MARKER_PREFIX && marker != MARKER_SOS && dataLength >= 0
            if (inHeader && marker == MARKER_APP1 && dataLength > XMP_HEADER.length) {
                val data = ByteArray(dataLength)
                raf.readFully(data)
                val text = String(data, Charsets.ISO_8859_1)
                if (text.startsWith(XMP_HEADER)) xmp = dataStart to text
            }
            raf.seek(dataStart + dataLength)
        }
        return xmp
    }
}
