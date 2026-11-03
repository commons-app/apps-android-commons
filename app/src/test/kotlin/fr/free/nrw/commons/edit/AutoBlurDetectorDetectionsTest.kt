package fr.free.nrw.commons.edit

import android.graphics.RectF
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class AutoBlurDetectorDetectionsTest {
    private val detections =
        AutoBlurDetector.Detections(
            bitmapWidth = 100,
            bitmapHeight = 50,
            bounds = listOf(RectF(10f, 5f, 30f, 25f)),
        )

    @Test
    fun sameSizeKeepsRegions() {
        assertEquals(listOf(RectF(10f, 5f, 30f, 25f)), detections.scaledTo(100, 50))
    }

    @Test
    fun largerTargetScalesRegionsUp() {
        assertEquals(listOf(RectF(20f, 10f, 60f, 50f)), detections.scaledTo(200, 100))
    }

    @Test
    fun differentRatiosScaleEachAxisSeparately() {
        assertEquals(listOf(RectF(30f, 2f, 90f, 10f)), detections.scaledTo(300, 20))
    }

    @Test
    fun detectedImageWithoutSizeGivesNoRegions() {
        val emptyImage = detections.copy(bitmapWidth = 0)

        assertTrue(emptyImage.scaledTo(200, 100).isEmpty())
    }
}
