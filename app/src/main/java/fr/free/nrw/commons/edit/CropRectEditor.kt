package fr.free.nrw.commons.edit

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Hit-tests crop handles and moves the crop rectangle inside its bounds.
 *
 * [cropRect] is the rectangle drawn by [CropOverlayView]. This editor mutates that same instance.
 */
internal class CropRectEditor(
    private val cropRect: RectF,
    private val minCropSize: Float,
    private val cornerTouchSlop: Float,
    private val edgeTouchSlop: Float,
) {
    /**
     * A corner, edge, or the interior of the crop rectangle that a drag can move.
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
        CENTER,
    }

    /**
     * Returns the handle under [x] and [y]. Corners win over edges, and the interior selects the center.
     */
    fun findHandle(
        x: Float,
        y: Float,
    ): Handle? {
        // Check corners first with larger touch zone (priority over edges)
        val corners =
            listOf(
                Triple(Handle.TOP_LEFT, cropRect.left, cropRect.top),
                Triple(Handle.TOP_RIGHT, cropRect.right, cropRect.top),
                Triple(Handle.BOTTOM_LEFT, cropRect.left, cropRect.bottom),
                Triple(Handle.BOTTOM_RIGHT, cropRect.right, cropRect.bottom),
            )
        val corner = corners.firstOrNull { (_, handleX, handleY) -> isNear(x, y, handleX, handleY, cornerTouchSlop) }

        // Then check edges with smaller touch zone
        val edges =
            listOf(
                Triple(Handle.TOP, cropRect.centerX(), cropRect.top),
                Triple(Handle.BOTTOM, cropRect.centerX(), cropRect.bottom),
                Triple(Handle.LEFT, cropRect.left, cropRect.centerY()),
                Triple(Handle.RIGHT, cropRect.right, cropRect.centerY()),
            )
        val edge =
            if (corner == null) {
                edges.firstOrNull { (_, handleX, handleY) -> isNear(x, y, handleX, handleY, edgeTouchSlop) }
            } else {
                null
            }

        // Check center last (for moving the entire crop area)
        return when {
            corner != null -> corner.first
            edge != null -> edge.first
            cropRect.contains(x, y) -> Handle.CENTER
            else -> null
        }
    }

    /**
     * Moves [handle] by [dx] and [dy], keeping the crop rectangle inside [bounds] and at least the minimum size.
     */
    fun moveHandle(
        handle: Handle,
        dx: Float,
        dy: Float,
        bounds: RectF,
    ) {
        when (handle) {
            Handle.TOP_LEFT -> {
                cropRect.left = constrainMin(cropRect.left + dx, bounds.left, cropRect.right - minCropSize)
                cropRect.top = constrainMin(cropRect.top + dy, bounds.top, cropRect.bottom - minCropSize)
            }
            Handle.TOP_RIGHT -> {
                cropRect.right = constrainMax(cropRect.right + dx, cropRect.left + minCropSize, bounds.right)
                cropRect.top = constrainMin(cropRect.top + dy, bounds.top, cropRect.bottom - minCropSize)
            }
            Handle.BOTTOM_LEFT -> {
                cropRect.left = constrainMin(cropRect.left + dx, bounds.left, cropRect.right - minCropSize)
                cropRect.bottom = constrainMax(cropRect.bottom + dy, cropRect.top + minCropSize, bounds.bottom)
            }
            Handle.BOTTOM_RIGHT -> {
                cropRect.right = constrainMax(cropRect.right + dx, cropRect.left + minCropSize, bounds.right)
                cropRect.bottom = constrainMax(cropRect.bottom + dy, cropRect.top + minCropSize, bounds.bottom)
            }
            Handle.TOP -> {
                cropRect.top = constrainMin(cropRect.top + dy, bounds.top, cropRect.bottom - minCropSize)
            }
            Handle.BOTTOM -> {
                cropRect.bottom = constrainMax(cropRect.bottom + dy, cropRect.top + minCropSize, bounds.bottom)
            }
            Handle.LEFT -> {
                cropRect.left = constrainMin(cropRect.left + dx, bounds.left, cropRect.right - minCropSize)
            }
            Handle.RIGHT -> {
                cropRect.right = constrainMax(cropRect.right + dx, cropRect.left + minCropSize, bounds.right)
            }
            Handle.CENTER -> moveCenter(dx, dy, bounds)
        }
    }

    /**
     * Shifts the whole crop rectangle by [dx] and [dy], sliding it back inside [bounds] so its size stays the same.
     */
    private fun moveCenter(
        dx: Float,
        dy: Float,
        bounds: RectF,
    ) {
        var newLeft = cropRect.left + dx
        var newTop = cropRect.top + dy
        var newRight = cropRect.right + dx
        var newBottom = cropRect.bottom + dy

        // Keep within bounds
        if (newLeft < bounds.left) {
            newRight += bounds.left - newLeft
            newLeft = bounds.left
        }
        if (newRight > bounds.right) {
            newLeft -= newRight - bounds.right
            newRight = bounds.right
        }
        if (newTop < bounds.top) {
            newBottom += bounds.top - newTop
            newTop = bounds.top
        }
        if (newBottom > bounds.bottom) {
            newTop -= newBottom - bounds.bottom
            newBottom = bounds.bottom
        }

        cropRect.set(newLeft, newTop, newRight, newBottom)
    }

    /**
     * Clamps [value] so a dragged edge cannot pass [min] or the opposite edge at [max].
     */
    private fun constrainMin(
        value: Float,
        min: Float,
        max: Float,
    ): Float = max(min, min(value, max))

    /**
     * Clamps [value] so a dragged edge cannot pass the opposite edge at [min] or the bound at [max].
     */
    private fun constrainMax(
        value: Float,
        min: Float,
        max: Float,
    ): Float = max(min, min(value, max))

    /**
     * Returns whether [x] and [y] lie within [slop] pixels of ([targetX], [targetY]).
     */
    private fun isNear(
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
}
