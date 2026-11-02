package fr.free.nrw.commons.edit

import android.graphics.RectF
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CropCoordinatesTest {
    private val displayedRect = RectF(0f, 0f, 200f, 100f)

    @Test
    fun cropCoveringDisplayedImageSelectsWholeFile() {
        val coordinates = fromViewCrop(RectF(0f, 0f, 200f, 100f), displayedRect)

        assertEquals(CropCoordinates(0, 0, FILE_WIDTH, FILE_HEIGHT), coordinates)
    }

    @Test
    fun cropInsideDisplayedImageIsScaledToFileSize() {
        val coordinates = fromViewCrop(RectF(50f, 25f, 150f, 75f), displayedRect)

        assertEquals(CropCoordinates(250, 125, 500, 250), coordinates)
    }

    @Test
    fun cropOutsideDisplayedImageIsClampedToFile() {
        val coordinates = fromViewCrop(RectF(-50f, -50f, 300f, 200f), displayedRect)

        assertEquals(CropCoordinates(0, 0, FILE_WIDTH, FILE_HEIGHT), coordinates)
    }

    @Test
    fun displayedImageOffsetIsSubtractedFromCrop() {
        val offsetDisplayedRect = RectF(100f, 40f, 300f, 140f)

        val coordinates = fromViewCrop(RectF(150f, 65f, 250f, 115f), offsetDisplayedRect)

        assertEquals(CropCoordinates(250, 125, 500, 250), coordinates)
    }

    @Test
    fun displayedImageWithoutWidthReturnsNull() {
        assertNull(fromViewCrop(RectF(10f, 10f, 20f, 20f), RectF(10f, 10f, 10f, 50f)))
    }

    @Test
    fun displayedImageWithoutHeightReturnsNull() {
        assertNull(fromViewCrop(RectF(10f, 10f, 20f, 20f), RectF(10f, 10f, 50f, 10f)))
    }

    @Test
    fun emptyCropAtRightEdgeKeepsOnePixelInsideFile() {
        val coordinates = fromViewCrop(RectF(200f, 0f, 200f, 100f), displayedRect)

        assertEquals(CropCoordinates(FILE_WIDTH - 1, 0, 1, FILE_HEIGHT), coordinates)
    }

    private fun fromViewCrop(
        viewCropRect: RectF,
        displayedRect: RectF,
    ) = CropCoordinates.fromViewCrop(viewCropRect, displayedRect, FILE_WIDTH, FILE_HEIGHT)

    private companion object {
        const val FILE_WIDTH = 1000
        const val FILE_HEIGHT = 500
    }
}
