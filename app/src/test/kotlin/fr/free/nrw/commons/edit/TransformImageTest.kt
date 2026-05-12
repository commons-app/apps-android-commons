package fr.free.nrw.commons.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.mediautil.image.jpeg.LLJTran
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream
import org.junit.Ignore

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

        // check a single pixel to ensure the rotation worked accurately
        val testX = w / 4
        val testY = h / 4

        val expectedColor = origBmp.getPixel(testX, testY)

        val (expectedX, expectedY) = when (rotationDegrees % 360) {
            90 -> Pair(h - testY - 1, testX)
            180 -> Pair(w - testX - 1, h - testY - 1)
            270 -> Pair(testY, w - testX - 1)
            0 -> Pair(testX, testY)
            else -> throw IllegalArgumentException("Unsupported rotation: $rotationDegrees")
        }

        val actualColor = rotBmp.getPixel(expectedX, expectedY)
        assertEquals(
            "pixel color at the ($testX, $testY) must exactly match rotated position ($expectedX, $expectedY)",
            expectedColor,
            actualColor
        )
    }

    private fun assertImagesAreIdentical(originalFile: File, finalFile: File) {
        val origBytes = originalFile.readBytes()
        val finalBytes = finalFile.readBytes()

        assertArrayEquals(
            "The two files must be exactly equal byte-for-byte",
            origBytes,
            finalBytes
        )
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
            val lljTran = LLJTran(file)
            try {
                lljTran.read(LLJTran.READ_ALL, false)
                val isPerfect = lljTran.checkPerfect(LLJTran.ROT_90, null) == 0
                if (isPerfect == expectPerfect) {
                    return file
                }
            } finally {
                lljTran.freeMemory()
            }
        }

        throw AssertionError("No ${if (expectPerfect) "perfect" else "imperfect"} JPEG test resource found")
    }

    @Ignore("Disabled due to ICC Color Profile brightness shift during rotation. see issue https://github.com/commons-app/apps-android-commons/issues/6659 ")
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

    @Test
    fun `rotateImage keeps lossless pixel rotation for perfect JPEG`() {
        val originalFile = getTempFileForRotationType(expectPerfect = true)
        val sourceExif = ExifInterface(originalFile.absolutePath)
        sourceExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        sourceExif.saveAttributes()

        val rotatedFile = transformImage.rotateImage(originalFile, 90, savePath)
        assertNotNull(rotatedFile)
        assertRotationWorked(originalFile, rotatedFile!!, 90)

        val rotatedExif = ExifInterface(rotatedFile.absolutePath)
        assertEquals(
            "Perfect JPEG path should write pixels rotated and reset orientation to normal",
            ExifInterface.ORIENTATION_NORMAL,
            rotatedExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )
    }

    @Test
    fun `rotateImage applies EXIF-only rotation for imperfect JPEG`() {
        val originalFile = getTempFileForRotationType(expectPerfect = false)

        val sourceExif = ExifInterface(originalFile.absolutePath)
        sourceExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        sourceExif.saveAttributes()

        val rotatedFile = transformImage.rotateImage(originalFile, 90, savePath)
        assertNotNull(rotatedFile)

        val rotatedExif = ExifInterface(rotatedFile!!.absolutePath)
        assertEquals(
            "Imperfect JPEG path should rotate via EXIF orientation only",
            ExifInterface.ORIENTATION_ROTATE_90,
            rotatedExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )

        val originalBitmap = decodeBitmap(originalFile)
        val rotatedBitmap = decodeBitmap(rotatedFile)
        assertEquals("EXIF-only rotation should keep pixel width unchanged", originalBitmap.width, rotatedBitmap.width)
        assertEquals("EXIF-only rotation should keep pixel height unchanged", originalBitmap.height, rotatedBitmap.height)

        val testX = originalBitmap.width / 3
        val testY = originalBitmap.height / 3
        assertEquals(
            "EXIF-only rotation should not change pixel matrix",
            originalBitmap.getPixel(testX, testY),
            rotatedBitmap.getPixel(testX, testY)
        )
    }

    @Test
    fun `rotateImage EXIF-only path applies relative rotation on current orientation`() {
        val originalFile = getTempFileForRotationType(expectPerfect = false)

        val sourceExif = ExifInterface(originalFile.absolutePath)
        sourceExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        sourceExif.saveAttributes()

        val rotatedFile = transformImage.rotateImage(originalFile, 180, savePath)
        assertNotNull(rotatedFile)

        val rotatedExif = ExifInterface(rotatedFile!!.absolutePath)
        assertEquals(
            "EXIF-only path should add relative rotation to current orientation",
            ExifInterface.ORIENTATION_ROTATE_270,
            rotatedExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )
    }

    @Test
    fun `rotateImage zero degree keeps EXIF orientation when already normal`() {
        val originalFile = getTempFileForRotationType(expectPerfect = false)

        val sourceExif = ExifInterface(originalFile.absolutePath)
        sourceExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        sourceExif.saveAttributes()

        val rotatedFile = transformImage.rotateImage(originalFile, 0, savePath)
        assertNotNull(rotatedFile)

        val rotatedExif = ExifInterface(rotatedFile!!.absolutePath)
        assertEquals(
            "Zero-degree save should keep NORMAL orientation",
            ExifInterface.ORIENTATION_NORMAL,
            rotatedExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )
    }

    @Test
    fun `rotateImage repeated saves returns to original orientation after full cycle`() {
        val originalFile = getTempFileForRotationType(expectPerfect = false)

        val sourceExif = ExifInterface(originalFile.absolutePath)
        sourceExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        sourceExif.saveAttributes()

        val fileAfterFirstSave = transformImage.rotateImage(originalFile, 90, savePath)
        assertNotNull(fileAfterFirstSave)

        val fileAfterSecondSave = transformImage.rotateImage(fileAfterFirstSave!!, 90, savePath)
        assertNotNull(fileAfterSecondSave)

        val fileAfterThirdSave = transformImage.rotateImage(fileAfterSecondSave!!, 180, savePath)
        assertNotNull(fileAfterThirdSave)

        val finalExif = ExifInterface(fileAfterThirdSave!!.absolutePath)
        assertEquals(
            "After 90 -> 90 -> 180 relative saves, EXIF orientation should return to NORMAL",
            ExifInterface.ORIENTATION_NORMAL,
            finalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )
    }

    @Test
    fun `rotateImage 180 then another 180 returns to normal for imperfect JPEG`() {
        val originalFile = getTempFileForRotationType(expectPerfect = false)
        val sourceExif = ExifInterface(originalFile.absolutePath)
        sourceExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        sourceExif.saveAttributes()

        val afterFirst180 = transformImage.rotateImage(originalFile, 180, savePath)
        assertNotNull(afterFirst180)

        val exifAfterFirst = ExifInterface(afterFirst180!!.absolutePath)
        assertEquals(
            "After first 180 save, EXIF should represent 180 orientation",
            ExifInterface.ORIENTATION_ROTATE_180,
            exifAfterFirst.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )

        val afterSecond180 = transformImage.rotateImage(afterFirst180, 180, savePath)
        assertNotNull(afterSecond180)

        val finalExif = ExifInterface(afterSecond180!!.absolutePath)
        assertEquals(
            "After 180 + 180, EXIF should return to NORMAL",
            ExifInterface.ORIENTATION_NORMAL,
            finalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        )
    }
}
