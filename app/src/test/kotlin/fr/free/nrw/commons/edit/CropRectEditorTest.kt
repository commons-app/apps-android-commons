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
class CropRectEditorTest {
    private val bounds = RectF(0f, 0f, 200f, 100f)

    @Test
    fun cornerWinsOverANearbyEdge() {
        val editor = editor(RectF(0f, 0f, 200f, 100f))

        assertEquals(CropRectEditor.Handle.TOP_LEFT, editor.findHandle(4f, 0f))
    }

    @Test
    fun edgeHitWhenThePointIsOutsideTheCornerSlop() {
        val editor = editor(RectF(0f, 0f, 200f, 100f))

        assertEquals(CropRectEditor.Handle.TOP, editor.findHandle(100f, 20f))
    }

    @Test
    fun interiorSelectsTheCenter() {
        val editor = editor(RectF(0f, 0f, 200f, 100f))

        assertEquals(CropRectEditor.Handle.CENTER, editor.findHandle(100f, 50f))
    }

    @Test
    fun pointOutsideTheRectangleAndHandlesIsMissed() {
        val editor = editor(RectF(0f, 0f, 200f, 100f))

        assertNull(editor.findHandle(100f, -30f))
    }

    @Test
    fun leftEdgeCannotPassTheMinimumSizeOrTheBound() {
        val crop = RectF(10f, 10f, 80f, 80f)
        val editor = editor(crop)

        editor.moveHandle(CropRectEditor.Handle.LEFT, dx = -40f, dy = 0f, bounds)

        assertEquals(RectF(0f, 10f, 80f, 80f), crop)
    }

    @Test
    fun bottomRightStopsAtTheBounds() {
        val crop = RectF(10f, 10f, 40f, 40f)
        val editor = editor(crop)

        editor.moveHandle(CropRectEditor.Handle.BOTTOM_RIGHT, dx = 100f, dy = 100f, RectF(0f, 0f, 100f, 100f))

        assertEquals(RectF(10f, 10f, 100f, 100f), crop)
    }

    @Test
    fun centerDragSlidesBackInsideTheBoundsWithoutChangingSize() {
        val crop = RectF(10f, 10f, 100f, 80f)
        val editor = editor(crop)

        editor.moveHandle(CropRectEditor.Handle.CENTER, dx = -50f, dy = 0f, bounds)

        assertEquals(RectF(0f, 10f, 90f, 80f), crop)
    }

    private fun editor(crop: RectF) =
        CropRectEditor(
            cropRect = crop,
            minCropSize = 20f,
            cornerTouchSlop = 10f,
            edgeTouchSlop = 20f,
        )
}
