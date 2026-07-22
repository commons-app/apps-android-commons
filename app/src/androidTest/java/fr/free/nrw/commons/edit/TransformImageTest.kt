package fr.free.nrw.commons.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.ajpegtran.blur.BlurRegion
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Tests for image transformation operations using Jpegtran.
 *
 * Note: These tests verify basic rotation and crop behavior.
 * The Jpegtran library requires Application Context; these tests use
 * AndroidJUnit4 to run as instrumented tests.
 */
@RunWith(AndroidJUnit4::class)
class TransformImageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private var context: Context? = null

    private val transformImage: TransformImage = TransformImageImpl()
    private lateinit var savePath: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        savePath = tempFolder.newFolder("cache")
    }

    @After
    fun tearDown() {
        transformImage.cleanup()
    }

    private fun rotateImage(imageFile: File, degree: Int, savePath: File = this.savePath): File {
        val normalizedDegree = ((degree % 360) + 360) % 360
        transformImage.initJpegtran(context!!, imageFile.absolutePath)
        val outPutFile = transformImage.rotateImage(imageFile, normalizedDegree, savePath)
        transformImage.cleanup()
        return outPutFile
    }

    private fun cropImage(
        imageFile: File,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        savePath: File = this.savePath
    ): File {
        transformImage.cleanup()
        transformImage.initJpegtran(context!!, imageFile.absolutePath)
        val outPutFile = transformImage.cropImage(left, top, width, height, savePath)
        transformImage.cleanup()
        return outPutFile
    }

    private fun blurImage(
        imageFile: File,
        regions: List<BlurRegion>,
        savePath: File = this.savePath
    ): File {
        transformImage.cleanup()
        transformImage.initJpegtran(context!!, imageFile.absolutePath)
        val outputFile = transformImage.blurImage(regions, savePath)
        transformImage.cleanup()
        return outputFile
    }

    private fun getResourceAsTempFile(fileName: String): File? {
        val resourceStream =
            javaClass.classLoader?.getResourceAsStream("exif/$fileName") ?: return null
        val tempFile = tempFolder.newFile("${System.currentTimeMillis()}_$fileName")
        FileOutputStream(tempFile).use { it.write(resourceStream.readBytes()) }
        return tempFile
    }

    private fun decodeBitmap(file: File): Bitmap {
        return BitmapFactory.decodeFile(file.absolutePath)
            ?: throw AssertionError("Failed to decode bitmap from ${file.name}")
    }

    private fun assertColorsAreSimilar(
        message: String,
        expected: Int,
        actual: Int,
        tolerance: Int = 25
    ) {
        val r1 = (expected shr 16) and 0xFF
        val g1 = (expected shr 8) and 0xFF
        val b1 = expected and 0xFF

        val r2 = (actual shr 16) and 0xFF
        val g2 = (actual shr 8) and 0xFF
        val b2 = actual and 0xFF

        val diffR = abs(r1 - r2)
        val diffG = abs(g1 - g2)
        val diffB = abs(b1 - b2)

        if (diffR > tolerance || diffG > tolerance || diffB > tolerance) {
            throw AssertionError(
                "$message - Color mismatch: expected R=$r1 G=$g1 B=$b1, but got R=$r2 G=$g2 B=$b2 (tolerance=$tolerance)"
            )
        }
    }

    private fun assertRotationWorked(originalFile: File, rotatedFile: File, rotationDegrees: Int) {
        val origBmp = decodeBitmap(originalFile)
        val rotBmp = decodeBitmap(rotatedFile)

        val w = origBmp.width
        val h = origBmp.height

        if (rotationDegrees % 180 != 0) {
            assertTrue("Width should be close to original height", abs(h - rotBmp.width) < 16)
            assertTrue("Height should be close to original width", abs(w - rotBmp.height) < 16)
        } else {
            assertTrue("Width should be close to original width", abs(w - rotBmp.width) < 16)
            assertTrue(
                "Height should be close to original height",
                abs(h - rotBmp.height) < 16
            )
        }
    }

    private fun assertImagesAreIdentical(originalFile: File, finalFile: File) {
        val origBmp = decodeBitmap(originalFile)
        val finalBmp = decodeBitmap(finalFile)

        assertTrue("Width should be close", abs(origBmp.width - finalBmp.width) < 16)
        assertTrue("Height should be close", abs(origBmp.height - finalBmp.height) < 16)
    }

    private fun getTempFileForRotationType(expectPerfect: Boolean): File {
        val candidates =
            listOf(
                "TEST_1.jpg",
                "TEST_IMAGE.jpg",
                "Landscape_1.jpg",
                "Landscape_2.jpg",
                "Landscape_3.jpg",
                "Portrait_1.jpg",
                "Portrait_2.jpg",
                "Portrait_3.jpg",
            )

        for (fileName in candidates) {
            val file = getResourceAsTempFile(fileName) ?: continue
            return file
        }

        throw AssertionError("No ${if (expectPerfect) "perfect" else "imperfect"} JPEG test resource found")
    }

    @Test
    fun test360DegreeRotationCyclesForAllExifImages() {
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
            val file90 = rotateImage(originalFile, 90)
            assertRotationWorked(originalFile, file90, 90)
            // 2. rotate 270 -> this should equal the original
            val file90then270 = rotateImage(file90, 270, savePath)
            assertImagesAreIdentical(originalFile, file90then270)

            // 3. rotate 180
            val file180 = rotateImage(originalFile, 180, savePath)
            assertRotationWorked(originalFile, file180, 180)
            // 4. rotate 180 -> this should equal the original
            val file180then180 = rotateImage(file180, 180, savePath)
            assertImagesAreIdentical(originalFile, file180then180)

            // 5. rotate 270
            val file270 = rotateImage(originalFile, 270, savePath)
            assertRotationWorked(originalFile, file270, 270)
            // 6. rotate 90 -> this should equal the original
            val file270then90 = rotateImage(file270, 90, savePath)
            assertImagesAreIdentical(originalFile, file270then90)

            // 7. rotate 360 -> this should equal the original
            val file360 = rotateImage(originalFile, 360, savePath)
            assertImagesAreIdentical(originalFile, file360)

            println("$fileName passed all rotation cycle tests")
        }
    }

    @Test
    fun rotateImageKeepsLosslessPixelRotationForPerfectJpeg() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)

        val rotatedFile = rotateImage(originalFile, 90, savePath)
        assertNotNull(rotatedFile)
        assertRotationWorked(originalFile, rotatedFile, 90)

        val rotatedExif = ExifInterface(rotatedFile.absolutePath)
        assertEquals(
            "Perfect JPEG path should write pixels rotated and reset orientation to normal",
            ExifInterface.ORIENTATION_NORMAL,
            rotatedExif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        )
    }

    @Test
    fun rotateImage180ThenAnother180ReturnsToNormalForImperfectJpeg() {
        val originalFile = getTempFileForRotationType(expectPerfect = false)

        val afterFirst180 = rotateImage(originalFile, 180, savePath)
        assertNotNull(afterFirst180)

        val exifAfterFirst = ExifInterface(afterFirst180.absolutePath)
        assertEquals(
            "After first 180 save, EXIF should represent NORMAL orientation because pixels are physically rotated",
            ExifInterface.ORIENTATION_NORMAL,
            exifAfterFirst.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        )
        assertRotationWorked(originalFile, afterFirst180, 180)

        val afterSecond180 = rotateImage(afterFirst180, 180, savePath)
        assertNotNull(afterSecond180)

        val finalExif = ExifInterface(afterSecond180.absolutePath)
        assertEquals(
            "After second 180 save, EXIF should return to NORMAL",
            ExifInterface.ORIENTATION_NORMAL,
            finalExif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        )
        assertImagesAreIdentical(originalFile, afterSecond180)
    }

    @Test
    fun rotateImageTrimsImperfectJpegToApplyLosslessRotation() {
        val originalFile = getTempFileForRotationType(expectPerfect = false)
        val originalBmp = decodeBitmap(originalFile)
        val originalWidth = originalBmp.width
        val originalHeight = originalBmp.height

        val rotatedFile = rotateImage(originalFile, 90)
        assertNotNull(rotatedFile)
        val rotatedBmp = decodeBitmap(rotatedFile)

        // Check if originalHeight is not divisible by 8 (or 16) - imperfect.
        val remH = originalHeight % 8
        if (remH != 0) {
            // The physical pixels must be trimmed to the nearest lower MCU boundary
            assertTrue(
                "Imperfect dimension should be trimmed and strictly smaller than original",
                rotatedBmp.width < originalHeight
            )
            assertTrue(
                "Trimmed amount should not exceed 16 pixels",
                rotatedBmp.width >= originalHeight - 16
            )
        }

        val remW = originalWidth % 8
        if (remW != 0) {
            assertTrue(
                "Imperfect dimension should be trimmed and strictly smaller than original",
                rotatedBmp.height < originalWidth
            )
            assertTrue(
                "Trimmed amount should not exceed 16 pixels",
                rotatedBmp.height >= originalWidth - 16
            )
        }
    }

    /*
    * Jpegtran physically rotates the image pixels, So no need to preserve the orientation.
    */
    @Test
    fun rotateImageResetsExifOrientation() {
        // Landscape_1.jpg has a non-normal EXIF orientation tag (usually ORIENTATION_ROTATE_90)
        val originalFile = getResourceAsTempFile("Landscape_1.jpg")
        assertNotNull(originalFile)

        // Rotate the image physically by 90 degrees
        val rotatedFile = rotateImage(originalFile!!, 90)
        assertNotNull(rotatedFile)

        val rotatedExif = ExifInterface(rotatedFile.absolutePath)
        assertEquals(
            "Because jpegtran physically rotates the pixels, the EXIF orientation tag must be reset to NORMAL (1) to prevent double-rotation in viewers",
            ExifInterface.ORIENTATION_NORMAL,
            rotatedExif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        )
    }

    @Test
    fun cropImageWithValidMcuBoundsProducesCorrectCroppedFile() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)
        val originalBmp = decodeBitmap(originalFile)
        val originalWidth = originalBmp.width
        val originalHeight = originalBmp.height

        val originalExif = ExifInterface(originalFile.absolutePath)
        val originalOrientation = originalExif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val cropLeft = 16
        val cropTop = 16
        val cropWidth = 160.coerceAtMost(originalWidth - cropLeft)
        val cropHeight = 160.coerceAtMost(originalHeight - cropTop)

        val croppedFile = cropImage(originalFile, cropLeft, cropTop, cropWidth, cropHeight)
        assertNotNull(croppedFile)

        // Verify mathematical dimensions
        val croppedBmp = decodeBitmap(croppedFile)
        assertEquals("Cropped width should match requested width", cropWidth, croppedBmp.width)
        assertEquals("Cropped height should match requested height", cropHeight, croppedBmp.height)

        // Verify file size is smaller than the original
        assertTrue(
            "Cropped file size should be smaller than original",
            croppedFile.length() < originalFile.length()
        )

        // Verify EXIF orientation is preserved
        val croppedExif = ExifInterface(croppedFile.absolutePath)
        val croppedOrientation = croppedExif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        assertEquals(
            "EXIF orientation should be preserved after crop",
            originalOrientation,
            croppedOrientation
        )

        // Verify visual pixel content matches the original region (with tolerance)
        assertColorsAreSimilar(
            "Top-left corner pixel of crop should match original",
            originalBmp.getPixel(cropLeft, cropTop),
            croppedBmp.getPixel(0, 0)
        )
        assertColorsAreSimilar(
            "Center pixel of crop should match original",
            originalBmp.getPixel(cropLeft + cropWidth / 2, cropTop + cropHeight / 2),
            croppedBmp.getPixel(cropWidth / 2, cropHeight / 2)
        )
        assertColorsAreSimilar(
            "Bottom-right corner pixel of crop should match original",
            originalBmp.getPixel(cropLeft + cropWidth - 1, cropTop + cropHeight - 1),
            croppedBmp.getPixel(cropWidth - 1, cropHeight - 1)
        )
    }

    @Test
    fun cropImagePreservesExifMetadata() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)
        val originalBmp = decodeBitmap(originalFile)

        // Read EXIF attributes on the original file directly
        val originalExif = ExifInterface(originalFile.absolutePath)

        val cropLeft = 16
        val cropTop = 16
        val cropWidth = 160.coerceAtMost(originalBmp.width - cropLeft)
        val cropHeight = 160.coerceAtMost(originalBmp.height - cropTop)

        val croppedFile = cropImage(originalFile, cropLeft, cropTop, cropWidth, cropHeight)
        assertNotNull(croppedFile)

        // Read EXIF attributes on the cropped file
        val croppedExif = ExifInterface(croppedFile.absolutePath)

        // Verify that standard EXIF tags are preserved if they are present in the original
        val tagsToCheck = listOf(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_IMAGE_DESCRIPTION
        )

        for (tag in tagsToCheck) {
            val originalValue = originalExif.getAttribute(tag)
            if (originalValue != null) {
                val croppedValue = croppedExif.getAttribute(tag)
                assertEquals(
                    "EXIF tag $tag should be preserved after crop",
                    originalValue,
                    croppedValue
                )
            }
        }
    }

    @Test
    fun cropImageAtVariousOffsetsIsValid() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)
        val originalBmp = decodeBitmap(originalFile)
        val originalWidth = originalBmp.width
        val originalHeight = originalBmp.height

        val originalExif = ExifInterface(originalFile.absolutePath)
        val originalOrientation = originalExif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val cropLeft = 32
        val cropTop = 48
        val cropWidth = 96.coerceAtMost(originalWidth - cropLeft)
        val cropHeight = 80.coerceAtMost(originalHeight - cropTop)

        val croppedFile = cropImage(originalFile, cropLeft, cropTop, cropWidth, cropHeight)
        assertNotNull(croppedFile)

        val croppedBmp = decodeBitmap(croppedFile)
        assertEquals(cropWidth, croppedBmp.width)
        assertEquals(cropHeight, croppedBmp.height)

        assertTrue(
            "Cropped file size should be smaller than original",
            croppedFile.length() < originalFile.length()
        )

        val croppedExif = ExifInterface(croppedFile.absolutePath)
        val croppedOrientation = croppedExif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        assertEquals(
            "EXIF orientation should be preserved after crop",
            originalOrientation,
            croppedOrientation
        )

        assertColorsAreSimilar(
            "Top-left corner pixel of crop should match original",
            originalBmp.getPixel(cropLeft, cropTop),
            croppedBmp.getPixel(0, 0)
        )
        assertColorsAreSimilar(
            "Center pixel of crop should match original",
            originalBmp.getPixel(cropLeft + cropWidth / 2, cropTop + cropHeight / 2),
            croppedBmp.getPixel(cropWidth / 2, cropHeight / 2)
        )
    }

    @Test
    fun blurImageProducesOutputFileWithSameDimensions() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)
        val originalBmp = decodeBitmap(originalFile)

        val blurRegion = BlurRegion(
            80, 80, 16, 16,
            100, 100, true
        )

        val blurredFile = blurImage(originalFile, listOf(blurRegion))
        assertNotNull(blurredFile)

        val blurredBmp = decodeBitmap(blurredFile)
        assertEquals(
            "Width should be unchanged after blur",
            originalBmp.width, blurredBmp.width
        )
        assertEquals(
            "Height should be unchanged after blur",
            originalBmp.height, blurredBmp.height
        )
    }

    @Test
    fun blurImageChangesPixelsInsideBlurRegion() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)
        val originalBmp = decodeBitmap(originalFile)

        val regionLeft = 16
        val regionTop = 16
        val regionW = 80
        val regionH = 80

        val blurRegion = BlurRegion(
            regionW, regionH, regionLeft, regionTop,
            100, 100, true
        )

        val blurredFile = blurImage(originalFile, listOf(blurRegion))
        assertNotNull(blurredFile)
        val blurredBmp = decodeBitmap(blurredFile)

        // Sample several pixels inside the blur region and count how many have changed.
        val samplePoints = listOf(
            Pair(regionLeft + regionW / 4, regionTop + regionH / 4),
            Pair(regionLeft + regionW / 2, regionTop + regionH / 2),
            Pair(regionLeft + 3 * regionW / 4, regionTop + 3 * regionH / 4),
            Pair(regionLeft + regionW / 4, regionTop + 3 * regionH / 4),
            Pair(regionLeft + 3 * regionW / 4, regionTop + regionH / 4)
        )

        var changedCount = 0
        for ((px, py) in samplePoints) {
            val origPixel = originalBmp.getPixel(px, py)
            val blurPixel = blurredBmp.getPixel(px, py)

            val dr = abs(((origPixel shr 16) and 0xFF) - ((blurPixel shr 16) and 0xFF))
            val dg = abs(((origPixel shr 8) and 0xFF) - ((blurPixel shr 8) and 0xFF))
            val db = abs((origPixel and 0xFF) - (blurPixel and 0xFF))

            if (dr > 2 || dg > 2 || db > 2) {
                changedCount++
            }
        }
        assertTrue(
            "At least one sampled pixel inside the blur region should have changed, but none did",
            changedCount > 0
        )
    }

    @Test
    fun blurImagePreservesPixelsOutsideBlurRegion() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)
        val originalBmp = decodeBitmap(originalFile)
        val w = originalBmp.width
        val h = originalBmp.height

        // Blur a small region in the center.
        val regionLeft = w / 4
        val regionTop = h / 4
        val regionW = w / 2
        val regionH = h / 2

        val blurRegion = BlurRegion(
            regionW, regionH, regionLeft, regionTop,
            100, 100, true
        )

        val blurredFile = blurImage(originalFile, listOf(blurRegion))
        assertNotNull(blurredFile)
        val blurredBmp = decodeBitmap(blurredFile)

        // Sample pixels well outside the blur region.
        val outsidePoints = listOf(
            Pair(2, 2),
            Pair(w - 3, 2),
            Pair(2, h - 3),
            Pair(w - 3, h - 3)
        )

        for ((px, py) in outsidePoints) {
            assertColorsAreSimilar(
                "Pixel at ($px,$py) outside blur region should be unchanged",
                originalBmp.getPixel(px, py),
                blurredBmp.getPixel(px, py)
            )
        }
    }

    @Test
    fun blurImagePreservesExifMetadata() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)

        val originalExif = ExifInterface(originalFile.absolutePath)

        val blurRegion = BlurRegion(
            80, 80, 16, 16,
            100, 100, true
        )

        val blurredFile = blurImage(originalFile, listOf(blurRegion))
        assertNotNull(blurredFile)

        val blurredExif = ExifInterface(blurredFile.absolutePath)

        val tagsToCheck = listOf(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_IMAGE_DESCRIPTION
        )

        for (tag in tagsToCheck) {
            val originalValue = originalExif.getAttribute(tag)
            if (originalValue != null) {
                val blurredValue = blurredExif.getAttribute(tag)
                assertEquals(
                    "EXIF tag $tag should be preserved after blur",
                    originalValue,
                    blurredValue
                )
            }
        }
    }
}
