package fr.free.nrw.commons.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

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
    private fun getAverageColor(bmp: Bitmap, centerX: Int, centerY: Int, radius: Int = 25): Int {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        val startX = (centerX - radius).coerceAtLeast(0)
        val endX = (centerX + radius).coerceAtMost(bmp.width - 1)
        val startY = (centerY - radius).coerceAtLeast(0)
        val endY = (centerY + radius).coerceAtMost(bmp.height - 1)

        for (x in startX..endX) {
            for (y in startY..endY) {
                val color = bmp.getPixel(x, y)
                rSum += Color.red(color)
                gSum += Color.green(color)
                bSum += Color.blue(color)
                count++
            }
        }

        return Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }
    private fun assertColorsMatch(expected: Int, actual: Int, tolerance: Int = 10) {
        val rExp = Color.red(expected)
        val gExp = Color.green(expected)
        val bExp = Color.blue(expected)

        val rAct = Color.red(actual)
        val gAct = Color.green(actual)
        val bAct = Color.blue(actual)

        // calculatee the uniform brightness shift across all the channels
        val shift = ((rAct - rExp) + (gAct - gExp) + (bAct - bExp)) / 3

        // normalise the actual colors by removing the the brightness shift
        val rNorm = rAct - shift
        val gNorm = gAct - shift
        val bNorm = bAct - shift

        val rDiff = abs(rExp - rNorm)
        val gDiff = abs(gExp - gNorm)
        val bDiff = abs(bExp - bNorm)

        if (rDiff > tolerance || gDiff > tolerance || bDiff > tolerance) {
            throw AssertionError(
                "Colors do not match within strict tolerance of $tolerance.\n" +
                        "Expected RGB($rExp, $gExp, $bExp)\n" +
                        "Actual RGB($rAct, $gAct, $bAct)\n" +
                        "(Note: A uniform brightness shift of $shift was detected and normalized)"
            )
        }
    }

    private fun assertRotationWorked(originalFile: File, rotatedFile: File, rotationDegrees: Int) {
        val origBmp = decodeBitmap(originalFile)
        val rotBmp = decodeBitmap(rotatedFile)

        val w = origBmp.width
        val h = origBmp.height

        val testX = w / 4
        val testY = h / 4

        val expectedColor = getAverageColor(origBmp, testX, testY)

        val (expectedX, expectedY) = when (rotationDegrees % 360) {
            90 -> Pair(h - testY - 1, testX)
            180 -> Pair(w - testX - 1, h - testY - 1)
            270 -> Pair(testY, w - testX - 1)
            0 -> Pair(testX, testY)
            else -> throw IllegalArgumentException("Unsupported rotation: $rotationDegrees")
        }

        if (rotationDegrees % 180 != 0) {
            assertEquals("Width/height should be swapped", h, rotBmp.width)
            assertEquals("Width/height should be swapped", w, rotBmp.height)
        } else {
            assertEquals("Dimensions should remain the same", w, rotBmp.width)
            assertEquals("Dimensions should remain the same", h, rotBmp.height)
        }

        val actualColor = getAverageColor(rotBmp, expectedX, expectedY)
        try {
            assertColorsMatch(expectedColor, actualColor)
        } catch (e: AssertionError) {
            throw AssertionError(
                "Region failed to map from ($testX, $testY) to ($expectedX, $expectedY) for $rotationDegrees degrees.\n${e.message}"
            )
        }
    }

    private fun assertImagesAreIdentical(originalFile: File, finalFile: File) {
        val origBmp = decodeBitmap(originalFile)
        val finalBmp = decodeBitmap(finalFile)

        assertEquals("Widths must match exactly", origBmp.width, finalBmp.width)
        assertEquals("Heights must match exactly", origBmp.height, finalBmp.height)

        val testX = origBmp.width / 4
        val testY = origBmp.height / 4

        assertColorsMatch(
            getAverageColor(origBmp, testX, testY),
            getAverageColor(finalBmp, testX, testY),
            tolerance = 10
        )
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