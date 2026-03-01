package fr.free.nrw.commons.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
}