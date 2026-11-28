package fr.free.nrw.commons.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Tests for [MotionPhotoUtils], using synthetic JPEG files that carry an XMP packet
 * and a video appended after the image data, as the motion photo format describes.
 */
class MotionPhotoUtilsTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeFile(bytes: ByteArray): File = tempFolder.newFile().apply { writeBytes(bytes) }

    @Test
    fun nonJpegFileIsLeftUntouched() {
        val bytes = "this is not a jpeg".toByteArray()
        val file = writeFile(bytes)

        assertFalse(MotionPhotoUtils.removeVideo(file))
        assertEquals(bytes.toList(), file.readBytes().toList())
    }

    @Test
    fun jpegWithoutXmpIsLeftUntouched() {
        val bytes = jpeg(xmpPayload = null, fillerSize = 20, videoSize = 100)
        val file = writeFile(bytes)

        assertFalse(MotionPhotoUtils.removeVideo(file))
        assertEquals(bytes.toList(), file.readBytes().toList())
    }

    @Test
    fun jpegWithUnrelatedXmpIsLeftUntouched() {
        val xmp = "<x:xmpmeta><rdf:RDF><rdf:Description rdf:about=\"\"/></rdf:RDF></x:xmpmeta>"
        val bytes = jpeg(xmpPayload = xmp, fillerSize = 20, videoSize = 100)
        val file = writeFile(bytes)

        assertFalse(MotionPhotoUtils.removeVideo(file))
        assertEquals(bytes.toList(), file.readBytes().toList())
    }

    @Test
    fun motionPhotoVideoIsRemovedAndPhotoMarkedAsStill() {
        val videoSize = 100
        val fillerSize = 20
        val bytes = jpeg(motionPhotoXmp(videoSize), fillerSize, videoSize)
        val file = writeFile(bytes)
        val originalSize = bytes.size

        assertTrue(MotionPhotoUtils.removeVideo(file))

        val result = file.readBytes()
        assertEquals(originalSize - videoSize, result.size)
        val text = String(result, Charsets.ISO_8859_1)
        // The flag is flipped to "0" so the photo no longer claims to be a motion photo.
        assertTrue(text.contains("GCamera:MotionPhoto=\"0\""))
        // The Container:Item entry is blanked out.
        assertFalse(text.contains("Item:Semantic=\"MotionPhoto\""))
        // The video itself is gone: nothing after the XMP/filler area can contain "ftyp".
        assertFalse(text.substringAfter(XMP_HEADER).contains("ftyp"))
    }

    @Test
    fun samsungHeaderBeforeVideoIsRemovedToo() {
        val videoSize = 100
        val fillerSize = 20
        val bytes = jpeg(motionPhotoXmp(videoSize), fillerSize, videoSize, samsungHeader = true)
        val file = writeFile(bytes)
        val originalSize = bytes.size

        assertTrue(MotionPhotoUtils.removeVideo(file))

        val result = file.readBytes()
        assertEquals(originalSize - videoSize - SAMSUNG_HEADER.size, result.size)
    }

    @Test
    fun microVideoIsRemoved() {
        val videoSize = 100
        val xmp =
            "<x:xmpmeta><rdf:RDF><rdf:Description rdf:about=\"\" " +
                "GCamera:MicroVideo=\"1\" GCamera:MicroVideoOffset=\"$videoSize\"/></rdf:RDF></x:xmpmeta>"
        val bytes = jpeg(xmp, fillerSize = 20, videoSize = videoSize)
        val file = writeFile(bytes)
        val originalSize = bytes.size

        assertTrue(MotionPhotoUtils.removeVideo(file))

        val result = file.readBytes()
        assertEquals(originalSize - videoSize, result.size)
        assertTrue(String(result, Charsets.ISO_8859_1).contains("GCamera:MicroVideo=\"0\""))
    }

    @Test
    fun declaredLengthNotMatchingTheFileEndLeavesTheFileUntouched() {
        // The XMP declares a 200-byte video but only 100 bytes follow: the computed video
        // start does not point at an ftyp box, so nothing may be cut.
        val bytes = jpeg(motionPhotoXmp(200), fillerSize = 20, videoSize = 100)
        val file = writeFile(bytes)

        assertFalse(MotionPhotoUtils.removeVideo(file))
        assertEquals(bytes.toList(), file.readBytes().toList())
    }

    @Test
    fun flagWithoutVideoLeavesTheFileUntouched() {
        // MotionPhoto flag and item present, but no video bytes at the end of the file.
        val bytes = jpeg(motionPhotoXmp(100), fillerSize = 20, videoSize = 0)
        val file = writeFile(bytes)

        assertFalse(MotionPhotoUtils.removeVideo(file))
        assertEquals(bytes.toList(), file.readBytes().toList())
    }

    private fun motionPhotoXmp(videoLength: Int): String =
        "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
            "<rdf:Description rdf:about=\"\" GCamera:MotionPhoto=\"1\">" +
            "<Container:Directory><rdf:Seq>" +
            "<rdf:li><Container:Item Item:Semantic=\"MotionPhoto\" Item:Length=\"$videoLength\"/></rdf:li>" +
            "</rdf:Seq></Container:Directory>" +
            "</rdf:Description></rdf:RDF></x:xmpmeta>"

    /**
     * Builds a minimal JPEG: the start-of-image marker, one APP1 segment carrying the XMP
     * packet, some filler bytes standing in for image data, then optionally the Samsung
     * header and a fake MP4 (size box plus "ftyp") of [videoSize] bytes at the very end.
     */
    private fun jpeg(
        xmpPayload: String?,
        fillerSize: Int,
        videoSize: Int,
        samsungHeader: Boolean = false,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0xFF)
        out.write(0xD8)
        if (xmpPayload != null) {
            val xmpBytes = (XMP_HEADER + xmpPayload).toByteArray(Charsets.ISO_8859_1)
            val segmentLength = xmpBytes.size + 2
            out.write(0xFF)
            out.write(0xE1)
            out.write(segmentLength shr 8)
            out.write(segmentLength and 0xFF)
            out.write(xmpBytes)
        }
        repeat(fillerSize) { out.write(0x00) }
        if (samsungHeader) out.write(SAMSUNG_HEADER)
        if (videoSize > 0) {
            val video = ByteArray(videoSize)
            video[4] = 'f'.code.toByte()
            video[5] = 't'.code.toByte()
            video[6] = 'y'.code.toByte()
            video[7] = 'p'.code.toByte()
            out.write(video)
        }
        return out.toByteArray()
    }

    private companion object {
        const val XMP_HEADER = "http://ns.adobe.com/xap/1.0/\u0000"
        val SAMSUNG_HEADER =
            "\u0000\u0000\u0030\u000A\u0010\u0000\u0000\u0000MotionPhoto_Data"
                .toByteArray(Charsets.ISO_8859_1)
    }
}
