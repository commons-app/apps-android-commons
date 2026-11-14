package fr.free.nrw.commons.edit

import android.graphics.Matrix
import android.graphics.Paint
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
class BlurRegionEditorTest {
    private val identity = Matrix()

    @Test
    fun cornerIsChosenBeforeTheEdgeOfASmallRectangle() {
        val regions = mutableListOf(RectF(0f, 0f, 10f, 10f))

        // The top edge's midpoint is also inside the corner slop, and corners are checked first.
        val handle = editor(regions).findResizeHandle(5f, 0f, identity)

        assertEquals(0 to BlurRegionEditor.Handle.TOP_LEFT, handle)
    }

    @Test
    fun edgeIsChosenWhenThePointIsAwayFromTheCorners() {
        val regions = mutableListOf(RectF(0f, 0f, 100f, 100f))

        val handle = editor(regions).findResizeHandle(50f, 0f, identity)

        assertEquals(0 to BlurRegionEditor.Handle.TOP, handle)
    }

    @Test
    fun emptyRegionIsSkipped() {
        val regions = mutableListOf(RectF(0f, 0f, 0f, 10f), RectF(20f, 20f, 80f, 80f))

        val handle = editor(regions).findResizeHandle(20f, 20f, identity)

        assertEquals(1 to BlurRegionEditor.Handle.TOP_LEFT, handle)
    }

    @Test
    fun pointAwayFromEveryHandleIsMissed() {
        val regions = mutableListOf(RectF(0f, 0f, 100f, 100f))

        assertNull(editor(regions).findResizeHandle(50f, 50f, identity))
    }

    @Test
    fun topLeftResizeKeepsTheMinimumSize() {
        val region = RectF(0f, 0f, 40f, 40f)
        val editor = editor(mutableListOf(region))

        editor.moveResizeHandle(0, BlurRegionEditor.Handle.TOP_LEFT, imgX = 35f, imgY = 35f)

        assertEquals(RectF(30f, 30f, 40f, 40f), region)
    }

    @Test
    fun unalignedRectExpandsToCoverWholeMcuBlocks() {
        val rect = RectF(10f, 10f, 30f, 40f)

        editor(mutableListOf(), mcuImage = { grid() }).snapRectToMCU(rect)

        assertEquals(RectF(0f, 0f, 32f, 48f), rect)
    }

    @Test
    fun alignedRectSnapsItsPositionAndKeepsItsSize() {
        val rect = RectF(20f, 18f, 52f, 50f)

        editor(mutableListOf(), mcuImage = { grid() }).snapRectToMCU(rect)

        assertEquals(RectF(16f, 16f, 48f, 48f), rect)
    }

    @Test
    fun alignedRectThatPassesTheImageEdgeSlidesBackInside() {
        val rect = RectF(90f, 0f, 122f, 16f)

        editor(mutableListOf(), mcuImage = { squareGrid(100) }).snapRectToMCU(rect)

        assertEquals(RectF(68f, 0f, 100f, 16f), rect)
    }

    @Test
    fun missingMcuImageLeavesTheRectUnchanged() {
        val rect = RectF(10f, 10f, 30f, 40f)

        editor(mutableListOf(), mcuImage = { null }).snapRectToMCU(rect)

        assertEquals(RectF(10f, 10f, 30f, 40f), rect)
    }

    @Test
    fun sourceOfReturnsNullWhenTheImageIsMissing() {
        assertNull(BlurRegionEditor.sourceOf(null, null))
    }

    private fun editor(
        regions: MutableList<RectF>,
        mcuImage: () -> McuImageSource? = { null },
    ) = BlurRegionEditor(
        regions = regions,
        cornerTouchSlop = 24f,
        edgeTouchSlop = 20f,
        minRegionSize = 10f,
        handlePaints = BlurHandlePaints(Paint(), Paint(), Paint()),
        mcuImage = mcuImage,
    )

    private fun grid() = squareGrid(160)

    private fun squareGrid(size: Int) =
        McuImageSource(
            mcuWidth = 16,
            mcuHeight = 16,
            width = size,
            height = size,
            drawableWidth = size,
            drawableHeight = size,
        )
}
