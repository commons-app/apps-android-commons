package fr.free.nrw.commons.edit

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import fr.free.nrw.commons.ajpegtran.Properties
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Size and highlight of one resize handle drawn on the blur overlay.
 */
internal data class BlurHandleMark(
    val radius: Float,
    val active: Boolean,
)

/**
 * Fills and borders for blur-region resize handles.
 */
internal data class BlurHandlePaints(
    val handle: Paint,
    val active: Paint,
    val border: Paint,
)

/**
 * JPEG MCU size and the drawable size used to snap a blur rectangle onto MCU blocks.
 */
internal data class McuImageSource(
    val mcuWidth: Int,
    val mcuHeight: Int,
    val width: Int,
    val height: Int,
    val drawableWidth: Int,
    val drawableHeight: Int,
)

/**
 * Hit-tests, moves, snaps, and draws the resize handles of blur regions.
 *
 * [regions] is the list stored by [BlurOverlayView]. This editor mutates those same rectangles.
 */
internal class BlurRegionEditor(
    private val regions: MutableList<RectF>,
    private val cornerTouchSlop: Float,
    private val edgeTouchSlop: Float,
    private val minRegionSize: Float,
    private val handlePaints: BlurHandlePaints,
    private val mcuImage: () -> McuImageSource?,
) {
    /**
     * A corner or edge of a blur rectangle that a drag can resize.
     */
    enum class Handle {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
    }

    /**
     * Returns the (regionIndex, Handle) pair for the first handle that is
     * close enough to the touch point [screenX], [screenY], or null.
     *
     * Corners are checked before edges so that they take priority when the
     * rectangle is very small.
     */
    fun findResizeHandle(
        screenX: Float,
        screenY: Float,
        ivMatrix: Matrix,
    ): Pair<Int, Handle>? {
        var match: Pair<Int, Handle>? = null
        for (i in regions.indices) {
            val region = regions[i]
            if (match == null && region.width() > 0 && region.height() > 0) {
                val handle = handleAt(region, screenX, screenY, ivMatrix)
                if (handle != null) {
                    match = i to handle
                }
            }
        }
        return match
    }

    /**
     * Moves the specified [handle] of region at [index] so that the
     * controlled edge/corner moves toward ([imgX], [imgY]) in image space.
     * A minimum size is enforced.
     */
    fun moveResizeHandle(
        index: Int,
        handle: Handle,
        imgX: Float,
        imgY: Float,
    ) {
        val region = regions[index]
        val minSize = minRegionSize

        when (handle) {
            Handle.TOP_LEFT -> {
                region.left = min(imgX, region.right - minSize)
                region.top = min(imgY, region.bottom - minSize)
            }
            Handle.TOP_RIGHT -> {
                region.right = max(imgX, region.left + minSize)
                region.top = min(imgY, region.bottom - minSize)
            }
            Handle.BOTTOM_LEFT -> {
                region.left = min(imgX, region.right - minSize)
                region.bottom = max(imgY, region.top + minSize)
            }
            Handle.BOTTOM_RIGHT -> {
                region.right = max(imgX, region.left + minSize)
                region.bottom = max(imgY, region.top + minSize)
            }
            Handle.TOP -> {
                region.top = min(imgY, region.bottom - minSize)
            }
            Handle.BOTTOM -> {
                region.bottom = max(imgY, region.top + minSize)
            }
            Handle.LEFT -> {
                region.left = min(imgX, region.right - minSize)
            }
            Handle.RIGHT -> {
                region.right = max(imgX, region.left + minSize)
            }
        }
    }

    /**
     * Snaps [rect] to MCU boundaries in drawable-pixel space.
     * If the rect's dimensions are already MCU-aligned, snaps position to the nearest MCU grid line,
     * While preserving size.
     * Otherwise, expands outward to cover full MCU blocks.
     */
    fun snapRectToMCU(rect: RectF) {
        // Pre-check.
        val image = mcuImage()?.takeIf { it.mcuWidth > 0 && it.mcuHeight > 0 } ?: return

        // MCU size in drawable-pixel space.
        val mcuW = image.mcuWidth.toFloat() * image.drawableWidth / image.width
        val mcuH = image.mcuHeight.toFloat() * image.drawableHeight / image.height
        val maxW = image.drawableWidth.toFloat()
        val maxH = image.drawableHeight.toFloat()

        val widthRemainder = rect.width() % mcuW
        val widthAligned = widthRemainder < MCU_SIZE_TOLERANCE || widthRemainder > (mcuW - MCU_SIZE_TOLERANCE)
        val heightRemainder = rect.height() % mcuH
        val heightAligned = heightRemainder < MCU_SIZE_TOLERANCE || heightRemainder > (mcuH - MCU_SIZE_TOLERANCE)

        // Already MCU-sized, Snap to nearest border position only, preserve size.
        if (widthAligned && heightAligned) {
            val w = rect.width()
            val h = rect.height()
            rect.left = round(rect.left / mcuW) * mcuW
            rect.top = round(rect.top / mcuH) * mcuH
            rect.right = rect.left + w
            rect.bottom = rect.top + h

            // Shift back if pushed past total image size.
            if (rect.right > maxW) {
                rect.offset(maxW - rect.right, 0f)
            }
            if (rect.bottom > maxH) {
                rect.offset(0f, maxH - rect.bottom)
            }
            if (rect.left < 0f) {
                rect.offset(-rect.left, 0f)
            }
            if (rect.top < 0f) {
                rect.offset(0f, -rect.top)
            }
        } else {
            // Not aligned, Expand outward to full MCU blocks.
            rect.left = floor(rect.left.toDouble() / mcuW).toFloat() * mcuW
            rect.top = floor(rect.top.toDouble() / mcuH).toFloat() * mcuH
            rect.right = ceil(rect.right.toDouble() / mcuW).toFloat() * mcuW
            rect.bottom = ceil(rect.bottom.toDouble() / mcuH).toFloat() * mcuH

            // Clamp to drawable bounds.
            rect.left = max(0f, rect.left)
            rect.top = max(0f, rect.top)
            rect.right = min(maxW, rect.right)
            rect.bottom = min(maxH, rect.bottom)
        }
    }

    /**
     * Maps an image-space point through [ivMatrix] to screen space and draws a
     * circular handle there.
     */
    fun drawHandleAt(
        canvas: Canvas,
        ivMatrix: Matrix,
        imgX: Float,
        imgY: Float,
        mark: BlurHandleMark,
    ) {
        val scratch = FloatArray(2)
        scratch[0] = imgX
        scratch[1] = imgY
        ivMatrix.mapPoints(scratch)
        val paint = if (mark.active) handlePaints.active else handlePaints.handle
        canvas.drawCircle(scratch[0], scratch[1], mark.radius, paint)
        canvas.drawCircle(scratch[0], scratch[1], mark.radius, handlePaints.border)
    }

    /**
     * Returns the corner or edge of [region] under the screen point, checking corners before edges.
     */
    private fun handleAt(
        region: RectF,
        screenX: Float,
        screenY: Float,
        ivMatrix: Matrix,
    ): Handle? {
        // Corners.
        val corners =
            listOf(
                Handle.TOP_LEFT to floatArrayOf(region.left, region.top),
                Handle.TOP_RIGHT to floatArrayOf(region.right, region.top),
                Handle.BOTTOM_LEFT to floatArrayOf(region.left, region.bottom),
                Handle.BOTTOM_RIGHT to floatArrayOf(region.right, region.bottom),
            )
        // Edges (midpoints).
        val edges =
            listOf(
                Handle.TOP to floatArrayOf(region.centerX(), region.top),
                Handle.BOTTOM to floatArrayOf(region.centerX(), region.bottom),
                Handle.LEFT to floatArrayOf(region.left, region.centerY()),
                Handle.RIGHT to floatArrayOf(region.right, region.centerY()),
            )
        return firstNear(corners, screenX, screenY, ivMatrix, cornerTouchSlop)
            ?: firstNear(edges, screenX, screenY, ivMatrix, edgeTouchSlop)
    }

    /**
     * Returns the first candidate whose image point maps within [slop] of the screen point.
     */
    private fun firstNear(
        candidates: List<Pair<Handle, FloatArray>>,
        screenX: Float,
        screenY: Float,
        ivMatrix: Matrix,
        slop: Float,
    ): Handle? {
        val point = FloatArray(2)
        var found: Handle? = null
        for ((handle, imagePoint) in candidates) {
            if (found != null) break
            point[0] = imagePoint[0]
            point[1] = imagePoint[1]
            ivMatrix.mapPoints(point)
            if (isNearScreen(screenX, screenY, point[0], point[1], slop)) {
                found = handle
            }
        }
        return found
    }

    /**
     * Returns whether [x] and [y] lie within [slop] pixels of ([targetX], [targetY]) in screen space.
     */
    private fun isNearScreen(
        x: Float,
        y: Float,
        targetX: Float,
        targetY: Float,
        slop: Float,
    ): Boolean {
        val dx = x - targetX
        val dy = y - targetY
        return dx * dx + dy * dy <= slop * slop
    }

    companion object {
        /** Remainder below this is treated as already aligned to an MCU block. */
        private const val MCU_SIZE_TOLERANCE = 0.01f

        /**
         * Reads the MCU grid from [properties] and [drawable], or returns null when either is missing.
         */
        fun sourceOf(
            properties: Properties?,
            drawable: Drawable?,
        ): McuImageSource? =
            if (properties == null || drawable == null) {
                null
            } else {
                McuImageSource(
                    mcuWidth = properties.MCU_Width,
                    mcuHeight = properties.MCU_Height,
                    width = properties.width,
                    height = properties.height,
                    drawableWidth = drawable.intrinsicWidth,
                    drawableHeight = drawable.intrinsicHeight,
                )
            }
    }
}
