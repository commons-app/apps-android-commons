package fr.free.nrw.commons.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class TransformImageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var transformImage: TransformImageImpl
    private lateinit var savePath: File

    @Before
    fun setUp() {
        transformImage = TransformImageImpl()
        savePath = tempFolder.newFolder("cache")
    }

    private fun getResourceAsTempFile(fileName: String): File? {
        val resourceStream = javaClass.classLoader?.getResourceAsStream("exif/$fileName") ?: return null
        val tempFile = tempFolder.newFile("${System.currentTimeMillis()}_$fileName")
        FileOutputStream(tempFile).use { it.write(resourceStream.readBytes()) }
        return tempFile
    }

    private fun decodeBitmap(file: File): Bitmap {
        return BitmapFactory.decodeFile(file.absolutePath)
            ?: throw AssertionError("Failed to decode bitmap from ${file.name}")
    }

    private fun assertRotationWorked(originalFile: File, rotatedFile: File, rotationDegrees: Int) {
        val origBmp = decodeBitmap(originalFile)
        val rotBmp = decodeBitmap(rotatedFile)

        val w = origBmp.width
        val h = origBmp.height

        if (rotationDegrees % 180 != 0) {
            assertEquals("Width/height should be swapped", h, rotBmp.width)
            assertEquals("Width/height should be swapped", w, rotBmp.height)
        } else {
            assertEquals("Dimensions should remain the same", w, rotBmp.width)
            assertEquals("Dimensions should remain the same", h, rotBmp.height)
        }
    }

    private fun assertImagesAreIdentical(originalFile: File, finalFile: File) {
        val origBmp = decodeBitmap(originalFile)
        val finalBmp = decodeBitmap(finalFile)

        assertEquals("Width must match", origBmp.width, finalBmp.width)
        assertEquals("Height must match", origBmp.height, finalBmp.height)

        val w = origBmp.width
        val h = origBmp.height
        val origPixels = IntArray(w * h)
        val finalPixels = IntArray(w * h)
        origBmp.getPixels(origPixels, 0, w, 0, 0, w, h)
        finalBmp.getPixels(finalPixels, 0, w, 0, 0, w, h)

        assertTrue(
            "All pixels must be identical after a full rotation cycle",
            origPixels.contentEquals(finalPixels)
        )
    }

    /**
     * Extracts the ICC profile bytes from a JPEG file by finding the APP2
     * segment with the "ICC_PROFILE" signature. Returns null if not found.
     */
    private fun extractIccProfile(file: File): ByteArray? {
        val data = file.readBytes()
        val sig = "ICC_PROFILE\u0000".toByteArray()
        var i = 2 // skip SOI
        while (i < data.size - 4) {
            if (data[i] != 0xFF.toByte()) break
            val marker = data[i + 1].toInt() and 0xFF
            if (marker == 0xDA || marker == 0xD9) break // SOS or EOI
            val length = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
            if (marker == 0xE2 && length > sig.size + 4) {
                val segStart = i + 4
                if (data.sliceArray(segStart until segStart + sig.size).contentEquals(sig)) {
                    // skip signature (12 bytes) + chunk number (1) + chunk count (1)
                    return data.sliceArray(segStart + sig.size + 2 until i + 2 + length)
                }
            }
            i += 2 + length
        }
        return null
    }

    @Test
    fun `test EXIF tags are preserved after rotation`() {
        val originalFile = getResourceAsTempFile("TEST_1.jpg")!!
        val originalExif = ExifInterface(originalFile.absolutePath)

        val tagsToCheck = arrayOf(
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_WHITE_BALANCE,
        )

        // Collect original values, filtering to tags that actually exist
        val originalValues = tagsToCheck.mapNotNull { tag ->
            originalExif.getAttribute(tag)?.let { tag to it }
        }
        assertTrue("TEST_1.jpg must have EXIF tags", originalValues.isNotEmpty())

        for (degree in listOf(90, 180, 270)) {
            val rotated = transformImage.rotateImage(originalFile, degree, savePath)!!
            val rotatedExif = ExifInterface(rotated.absolutePath)

            for ((tag, originalValue) in originalValues) {
                val rotatedValue = rotatedExif.getAttribute(tag)
                assertEquals(
                    "EXIF tag $tag must be preserved after ${degree}° rotation",
                    originalValue,
                    rotatedValue
                )
            }
        }
    }

    /**
     * Injects a minimal sRGB ICC profile into a JPEG file by inserting an APP2
     * segment right after the SOI marker. Returns a new temp file.
     */
    private fun injectIccProfile(sourceFile: File): File {
        val data = sourceFile.readBytes()
        val out = ByteArrayOutputStream(data.size + 200)

        // Write SOI
        out.write(data, 0, 2)

        // Build a minimal 128-byte ICC profile header (sRGB)
        val iccProfile = ByteArray(128)
        // Profile size (4 bytes big-endian)
        iccProfile[0] = 0; iccProfile[1] = 0; iccProfile[2] = 0; iccProfile[3] = 128.toByte()
        // Profile version: 2.1.0
        iccProfile[8] = 2; iccProfile[9] = 0x10.toByte()
        // Device class: 'mntr' (monitor)
        "mntr".toByteArray().copyInto(iccProfile, 12)
        // Color space: 'RGB '
        "RGB ".toByteArray().copyInto(iccProfile, 16)
        // PCS: 'XYZ '
        "XYZ ".toByteArray().copyInto(iccProfile, 20)
        // 'acsp' signature (required at offset 36)
        "acsp".toByteArray().copyInto(iccProfile, 36)

        // APP2 marker
        out.write(0xFF)
        out.write(0xE2)
        // ICC_PROFILE signature (12) + chunk num (1) + chunk count (1) + profile data
        val segPayload = 2 + 12 + 1 + 1 + iccProfile.size // length field + sig + chunks + data
        out.write((segPayload shr 8) and 0xFF)
        out.write(segPayload and 0xFF)
        out.write("ICC_PROFILE\u0000".toByteArray())
        out.write(1) // chunk number
        out.write(1) // total chunks
        out.write(iccProfile)

        // Write rest of original file (everything after SOI)
        out.write(data, 2, data.size - 2)

        val outFile = tempFolder.newFile("icc_injected_${System.currentTimeMillis()}.jpg")
        outFile.writeBytes(out.toByteArray())
        return outFile
    }

    @Test
    fun `test ICC color profile is preserved after rotation`() {
        val baseFile = getResourceAsTempFile("Landscape_0.jpg")!!
        val originalFile = injectIccProfile(baseFile)
        val originalIcc = extractIccProfile(originalFile)
        assertTrue("Injected image must have an ICC profile", originalIcc != null && originalIcc.isNotEmpty())

        for (degree in listOf(90, 180, 270)) {
            val rotated = transformImage.rotateImage(originalFile, degree, savePath)!!
            val rotatedIcc = extractIccProfile(rotated)
            assertTrue(
                "ICC profile must be preserved after ${degree}° rotation",
                rotatedIcc != null && originalIcc!!.contentEquals(rotatedIcc)
            )
        }
    }

    @Test
    fun `test 360 degree rotation cycles for all EXIF images`() {
        val prefixes = listOf("Landscape", "Portrait", "TEST")
        val fileNamesToTest = mutableListOf<String>()

        for (prefix in prefixes) {
            for (i in 0..8) {
                fileNamesToTest.add("${prefix}_$i.jpg")
            }
        }
        fileNamesToTest.add("TEST_IMAGE.jpg")

        for (fileName in fileNamesToTest) {
            val originalFile = getResourceAsTempFile(fileName) ?: continue

            // 1. rotate 90
            val file90 = transformImage.rotateImage(originalFile, 90, savePath)!!
            assertRotationWorked(originalFile, file90, 90)
            // 2. rotate 270 -> this should equal the original
            val file90then270 = transformImage.rotateImage(file90, 270, savePath)!!
            assertImagesAreIdentical(originalFile, file90then270)

            // 3. rotate 180
            val file180 = transformImage.rotateImage(originalFile, 180, savePath)!!
            assertRotationWorked(originalFile, file180, 180)
            // 4. rotate 180 -> this should equal the original
            val file180then180 = transformImage.rotateImage(file180, 180, savePath)!!
            assertImagesAreIdentical(originalFile, file180then180)

            // 5. rotate 270
            val file270 = transformImage.rotateImage(originalFile, 270, savePath)!!
            assertRotationWorked(originalFile, file270, 270)
            // 6. rotate 90 -> thishould equal the original
            val file270then90 = transformImage.rotateImage(file270, 90, savePath)!!
            assertImagesAreIdentical(originalFile, file270then90)

            // 7. rotate 360 -> this should equal the original
            val file360 = transformImage.rotateImage(originalFile, 360, savePath)!!
            assertImagesAreIdentical(originalFile, file360)

            println("$fileName passed all rotation cycle tests")
        }
    }
}
