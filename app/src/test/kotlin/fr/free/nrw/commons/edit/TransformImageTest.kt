package fr.free.nrw.commons.edit

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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

    @Test
    fun testRotateImage_checksAllExifOrientations() {
        val prefixes = listOf("Landscape", "Portrait")
        val rotationsToTest = listOf(0, 90)

        for (prefix in prefixes) {
            for (i in 1..8) {
                val filename = "${prefix}_$i.jpg"
                println("Testing file: $filename")

                //setup the the test File
                val resourceStream = javaClass.classLoader?.getResourceAsStream("exif/$filename")
                assertNotNull("Could not find test image: $filename", resourceStream)

                val inputFile = File(tempFolder.root, filename)
                FileOutputStream(inputFile).use { output ->
                    resourceStream!!.copyTo(output)
                }

                //read the original orientation
                val originalExif = ExifInterface(inputFile.absolutePath)
                val originalOrientation = originalExif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                //test the rotations
                for (degree in rotationsToTest) {
                    val output = transformImage.rotateImage(inputFile, degree, savePath)

                    assertNotNull(output)
                    assertTrue(output!!.exists())

                    val outputExif = ExifInterface(output.absolutePath)
                    val outputOrientation = outputExif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

                    if (degree == 0) {
                        //case 0: file copy. orientation must be unchanged
                        assertEquals(
                            "For 0-degree rotation of $filename, orientation should be preserved",
                            originalOrientation,
                            outputOrientation
                        )
                    } else {
                        //case 90: LLJTran. orientation must be here normalized to the 1
                        assertEquals(
                            "For 90-degree rotation of $filename, output should be Normalized (1)",
                            ExifInterface.ORIENTATION_NORMAL,
                            outputOrientation
                        )
                    }
                }
            }
        }
    }
}