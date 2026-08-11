package fr.free.nrw.commons.utils

import androidx.exifinterface.media.ExifInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for [RandomAccessFileExifWriter].
 */
class RandomAccessFileExifWriterTest {

    private val sampleImagePath = "src/test/resources/ImageTest/dark1.jpg"
    private lateinit var testFile: File

    @Before
    fun setUp() {
        val sampleFile = File(sampleImagePath)
        testFile = File.createTempFile("exif_test_", ".jpg")
        sampleFile.copyTo(testFile, overwrite = true)
    }

    @After
    fun tearDown() {
        if (::testFile.isInitialized && testFile.exists()) {
            testFile.delete()
        }
    }

    @Test
    fun `writeTag updates orientation`() {
        RandomAccessFileExifWriter.writeTag(
            testFile, ExifInterface.TAG_ORIENTATION, "6"
        )
        val after = ExifInterface(testFile.absolutePath)
        assertEquals("6", after.getAttribute(ExifInterface.TAG_ORIENTATION))
    }

    @Test
    fun `writeTag removes a tag when value is null`() {
        // Write a tag first to ensure it's set
        RandomAccessFileExifWriter.writeTag(
            testFile, ExifInterface.TAG_ORIENTATION, "1"
        )
        val before = ExifInterface(testFile.absolutePath)
        assertNotNull(before.getAttribute(ExifInterface.TAG_ORIENTATION))

        // Remove tag by passing null
        RandomAccessFileExifWriter.writeTag(
            testFile, ExifInterface.TAG_ORIENTATION, null
        )
        val after = ExifInterface(testFile.absolutePath)
        assertNull(after.getAttribute(ExifInterface.TAG_ORIENTATION))
    }

    @Test
    fun `writeTag does nothing on non-existent file`() {
        val fake = File("src/test/resources/non_existent_file.jpg")
        RandomAccessFileExifWriter.writeTag(fake, ExifInterface.TAG_ORIENTATION, "1")
    }

    @Test
    fun `writeTag does nothing for unknown tag name`() {
        RandomAccessFileExifWriter.writeTag(testFile, "UnknownTagName", "123")
    }

    @Test
    fun `writeTags updates multiple tags at once`() {
        RandomAccessFileExifWriter.writeTags(
            testFile, mapOf(
                ExifInterface.TAG_ORIENTATION to "3",
                ExifInterface.TAG_MAKE to "TestMake"
            )
        )

        val after = ExifInterface(testFile.absolutePath)
        assertEquals("3", after.getAttribute(ExifInterface.TAG_ORIENTATION))
        assertEquals("TestMake", after.getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun `writeTags with empty map does nothing`() {
        val sizeBefore = testFile.length()
        RandomAccessFileExifWriter.writeTags(testFile, emptyMap())
        assertEquals(sizeBefore, testFile.length())
    }

    @Test
    fun `removeLocation strips GPS tags`() {
        // Set a GPS tag first
        RandomAccessFileExifWriter.writeTag(
            testFile, ExifInterface.TAG_GPS_LATITUDE_REF, "N"
        )
        val before = ExifInterface(testFile.absolutePath)
        assertNotNull(before.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))

        // Remove location
        RandomAccessFileExifWriter.removeLocation(testFile)

        val after = ExifInterface(testFile.absolutePath)
        assertNull(after.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
    }

    @Test
    fun `redactTags removes specified tags`() {
        RandomAccessFileExifWriter.writeTags(
            testFile, mapOf(
                ExifInterface.TAG_ORIENTATION to "1",
                ExifInterface.TAG_MAKE to "TestMake"
            )
        )

        RandomAccessFileExifWriter.redactTags(
            testFile, setOf(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_MAKE
            )
        )

        val after = ExifInterface(testFile.absolutePath)
        assertNull(after.getAttribute(ExifInterface.TAG_ORIENTATION))
        assertNull(after.getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun `copyExif copies tags from ExifInterface to destination file`() {
        val sampleFile = File(sampleImagePath)
        val dstFile = File.createTempFile("exif_dst_", ".jpg")
        try {
            sampleFile.copyTo(dstFile, overwrite = true)

            // Set tag on source file
            RandomAccessFileExifWriter.writeTag(
                testFile, ExifInterface.TAG_ORIENTATION, "6"
            )

            val srcExif = ExifInterface(testFile.absolutePath)
            RandomAccessFileExifWriter.copyExif(srcExif, dstFile)

            val dstExif = ExifInterface(dstFile.absolutePath)
            assertEquals("6", dstExif.getAttribute(ExifInterface.TAG_ORIENTATION))
        } finally {
            dstFile.delete()
        }
    }
}