package fr.free.nrw.commons.edit

import android.content.Intent
import android.view.View
import android.widget.SeekBar
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ajpegtran.blur.BlurRegion
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * On-device checks for blur strength in [EditActivity].
 */
@RunWith(AndroidJUnit4::class)
class EditBlurPixelSizeActivityTest {
    private lateinit var imageFile: File
    private var scenario: ActivityScenario<EditActivity>? = null

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        imageFile = File(context.cacheDir, "blur_strength_ui_test.jpg")
        val bytes =
            javaClass.classLoader!!
                .getResourceAsStream("exif/TEST_1.jpg")!!
                .readBytes()
        FileOutputStream(imageFile).use { it.write(bytes) }
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
        imageFile.delete()
    }

    @Test
    fun blurModeShowsStrengthSeekBarAtMaxDefault() {
        scenario = launchEditActivity()
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        scenario!!.onActivity { activity ->
            assertEquals(View.GONE, activity.findViewById<View>(R.id.blur_pixel_size_layout).visibility)
            activity.findViewById<View>(R.id.blur_btn).performClick()
        }
        instrumentation.waitForIdleSync()

        scenario!!.onActivity { activity ->
            val layout = activity.findViewById<View>(R.id.blur_pixel_size_layout)
            assertEquals(View.VISIBLE, layout.visibility)
            val seek = activity.findViewById<SeekBar>(R.id.blur_pixel_size_seek)
            assertEquals(BlurStrength.MAX_PROGRESS, seek.max)
            assertEquals(BlurStrength.DEFAULT_PROGRESS, seek.progress)
        }
    }

    @Test
    fun midStrengthKeepsMoreDetailThanMax() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm = EditViewModel()
        vm.initJpegtran(context, imageFile.absolutePath)
        val savePath = context.cacheDir
        val props = vm.refreshProperties()
        val width = props.width
        val height = props.height
        val regionW = (width / 2).coerceAtLeast(64)
        val regionH = (height / 2).coerceAtLeast(64)
        val cornerX = (width - regionW) / 4
        val cornerY = (height - regionH) / 4
        val region =
            BlurRegion(
                regionW,
                regionH,
                cornerX,
                cornerY,
                -1,
                -1,
                true,
            )

        val maxFile = vm.blurImage(listOf(region), savePath, strength = 100, context = context)
        vm.cleanup()
        vm.initJpegtran(context, imageFile.absolutePath)
        val midFile = vm.blurImage(listOf(region), savePath, strength = 50, context = context)
        vm.cleanup()

        val maxColors = uniqueColorsInRegion(maxFile, cornerX, cornerY, regionW, regionH)
        val midColors = uniqueColorsInRegion(midFile, cornerX, cornerY, regionW, regionH)

        assertEquals(1, maxColors)
        assertTrue(
            "50% strength should keep more than one color (was $midColors)",
            midColors > 1,
        )
    }

    private fun launchEditActivity(): ActivityScenario<EditActivity> {
        val intent =
            Intent(
                ApplicationProvider.getApplicationContext(),
                EditActivity::class.java,
            ).putExtra("image", imageFile.absolutePath)
        return ActivityScenario.launch(intent)
    }

    private fun uniqueColorsInRegion(
        file: File,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
    ): Int {
        val bitmap =
            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                ?: throw AssertionError("Could not decode ${file.absolutePath}")
        val colors = HashSet<Int>()
        val stepX = (width / 8).coerceAtLeast(1)
        val stepY = (height / 8).coerceAtLeast(1)
        var y = top
        while (y < top + height && y < bitmap.height) {
            var x = left
            while (x < left + width && x < bitmap.width) {
                colors.add(bitmap.getPixel(x, y))
                x += stepX
            }
            y += stepY
        }
        bitmap.recycle()
        return colors.size
    }
}
