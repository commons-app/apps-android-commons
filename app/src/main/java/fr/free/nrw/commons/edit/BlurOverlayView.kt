package fr.free.nrw.commons.edit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import fr.free.nrw.commons.ajpegtran.blur.BlurRegion
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.withMatrix

/**
 * Custom overlay view to allow users to draw and select multiple rectangular
 * regions on top of an image. Supports pinch-to-zoom (two fingers) and
 * double-tap to reset while single-finger drag draws blur rectangles.
 * Resize rectangle by pointed edges and corners.
 * All drawn regions are stored in raw image pixel coordinates. Zoom and pan
 * are applied directly to the ImageView's imageMatrix so that the overlay
 * drawing and the image are always perfectly synchronized.
 */
class BlurOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val regions = ArrayList<RectF>()
    private var currentActiveBox: RectF? = null
    private var startX = 0f
    private var startY = 0f
    private lateinit var borderPaint: Paint
    private lateinit var fillPaint: Paint
    private var isScaling = false
    private var lastMidX = 0f
    private var lastMidY = 0f
    private var baseMatrix = Matrix()
    private val deleteMarkerRadiusDp = 6f
    private val deleteMarkerXSizeDp = 2f
    private val deleteMarkerStrokeWidthDp = 1f
    private val deleteMarkerTouchRadiusDp = 10f
    private var density = 0f
    private lateinit var deleteCirclePaint: Paint
    private lateinit var deleteXPaint: Paint
    private var deleteBoxIndex = -1
    private var startScreenX = 0f
    private var startScreenY = 0f
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var doubleTapDetector: GestureDetector
    private var imageView: ImageView? = null
    private var resizeRegionIndex = -1
    private var resizeHandle: Handle? = null
    private val cornerHandleRadiusDp = 4f
    private val edgeHandleRadiusDp = 2f
    private val cornerTouchSlopDp = 24f
    private val edgeTouchSlopDp = 20f
    private val minRegionSizeDp = 20f
    private lateinit var handlePaint: Paint
    private lateinit var activeHandlePaint: Paint
    private lateinit var handleBorderPaint: Paint

    private enum class Handle {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT
    }

    fun setImageView(imageView: ImageView) {
        this.imageView = imageView
    }

    /**
     * Called when entering blur mode save the current imageMatrix.
     */
    fun setBaseMatrix(matrix: Matrix) {
        baseMatrix = Matrix(matrix)
    }

    init {
        initView(context)
    }

    private fun initView(context: Context) {

        density = context.resources.displayMetrics.density

        borderPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 4.0f
            isAntiAlias = true
        }

        fillPaint = Paint().apply {
            color = Color.argb(80, 0, 255, 255)
            style = Paint.Style.FILL
        }

        deleteCirclePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        deleteXPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = deleteMarkerStrokeWidthDp * density
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        handlePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        activeHandlePaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        handleBorderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            isAntiAlias = true
        }

        // Pinch-to-zoom.
        scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    val iv = imageView ?: return true
                    val m = Matrix(iv.imageMatrix)
                    val factor = d.scaleFactor
                    m.postScale(factor, factor, d.focusX, d.focusY)
                    iv.imageMatrix = m
                    invalidate()
                    return true
                }
            })
        scaleDetector.isQuickScaleEnabled = false

        // Double-tap to reset zoom back to baseMatrix.
        doubleTapDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    resetZoom()
                    return true
                }
            })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val ivMatrix = imageView?.imageMatrix ?: return

        // Draw rectangles in image-pixel space via the shared imageMatrix.
        canvas.withMatrix(ivMatrix) {
            for (rect in regions) {
                drawRect(rect, fillPaint)
                drawRect(rect, borderPaint)
            }
            currentActiveBox?.let {
                drawRect(it, fillPaint)
                drawRect(it, borderPaint)
            }
        }

        // Resize handles.
        val cornerRadius = cornerHandleRadiusDp * density
        val edgeRadius = edgeHandleRadiusDp * density
        val pts = FloatArray(2)

        for (i in regions.indices) {
            val rect = regions[i]
            if (rect.width() <= 0 || rect.height() <= 0) continue

            val isActiveRect = (i == resizeRegionIndex)

            // Corner handles
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.left,
                rect.top,
                cornerRadius,
                isActiveRect && resizeHandle == Handle.TOP_LEFT,
                pts
            )
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.right,
                rect.top,
                cornerRadius,
                isActiveRect && resizeHandle == Handle.TOP_RIGHT,
                pts
            )
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.left,
                rect.bottom,
                cornerRadius,
                isActiveRect && resizeHandle == Handle.BOTTOM_LEFT,
                pts
            )
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.right,
                rect.bottom,
                cornerRadius,
                isActiveRect && resizeHandle == Handle.BOTTOM_RIGHT,
                pts
            )

            // Edge (midpoint) handles.
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.centerX(),
                rect.top,
                edgeRadius,
                isActiveRect && resizeHandle == Handle.TOP,
                pts
            )
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.centerX(),
                rect.bottom,
                edgeRadius,
                isActiveRect && resizeHandle == Handle.BOTTOM,
                pts
            )
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.left,
                rect.centerY(),
                edgeRadius,
                isActiveRect && resizeHandle == Handle.LEFT,
                pts
            )
            drawHandleAt(
                canvas,
                ivMatrix,
                rect.right,
                rect.centerY(),
                edgeRadius,
                isActiveRect && resizeHandle == Handle.RIGHT,
                pts
            )
        }

        // Delete markers.
        val markerRadius = deleteMarkerRadiusDp * density
        val markerXSize = deleteMarkerXSizeDp * density
        val cornerPt = FloatArray(2)

        for (rect in regions) {
            if (rect.width() > 0 && rect.height() > 0) {
                cornerPt[0] = rect.right
                cornerPt[1] = rect.top
                ivMatrix.mapPoints(cornerPt)
                val cx = cornerPt[0]
                val cy = cornerPt[1]

                canvas.drawCircle(cx, cy, markerRadius, deleteCirclePaint)
                canvas.drawLine(
                    cx - markerXSize, cy - markerXSize,
                    cx + markerXSize, cy + markerXSize, deleteXPaint
                )
                canvas.drawLine(
                    cx + markerXSize, cy - markerXSize,
                    cx - markerXSize, cy + markerXSize, deleteXPaint
                )
            }
        }
    }

    /**
     * Maps an image-space point through [ivMatrix] to screen space and draws a
     * circular handle there.
     */
    private fun drawHandleAt(
        canvas: Canvas,
        ivMatrix: Matrix,
        imgX: Float,
        imgY: Float,
        radius: Float,
        isActive: Boolean,
        scratch: FloatArray
    ) {
        scratch[0] = imgX
        scratch[1] = imgY
        ivMatrix.mapPoints(scratch)
        val paint = if (isActive) activeHandlePaint else handlePaint
        canvas.drawCircle(scratch[0], scratch[1], radius, paint)
        canvas.drawCircle(scratch[0], scratch[1], radius, handleBorderPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        doubleTapDetector.onTouchEvent(event)

        // Two-finger gesture: pan.
        if (event.pointerCount >= 2) {
            isScaling = true
            currentActiveBox = null
            deleteBoxIndex = -1
            resizeRegionIndex = -1
            resizeHandle = null

            val midX = (event.getX(0) + event.getX(1)) / 2f
            val midY = (event.getY(0) + event.getY(1)) / 2f

            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                val iv = imageView ?: return true
                val dx = midX - lastMidX
                val dy = midY - lastMidY
                val m = Matrix(iv.imageMatrix)
                m.postTranslate(dx, dy)
                iv.imageMatrix = m
                invalidate()
            }

            lastMidX = midX
            lastMidY = midY
            return true
        }

        if (isScaling) {
            if (event.actionMasked == MotionEvent.ACTION_UP) isScaling = false
            return true
        }

        // Single-finger gesture - resize, draw, delete
        val ivMatrix = imageView?.imageMatrix ?: Matrix()
        val inv = Matrix()
        ivMatrix.invert(inv)

        val pt = floatArrayOf(event.x, event.y)
        inv.mapPoints(pt)
        val x = pt[0]
        val y = pt[1]

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                startScreenX = event.x
                startScreenY = event.y
                deleteBoxIndex = -1
                resizeRegionIndex = -1
                resizeHandle = null

                // Check delete markers first.
                val touchRadius = deleteMarkerTouchRadiusDp * density
                val cornerPt = FloatArray(2)
                for (i in regions.indices) {
                    cornerPt[0] = regions[i].right
                    cornerPt[1] = regions[i].top
                    ivMatrix.mapPoints(cornerPt)

                    val dx = event.x - cornerPt[0]
                    val dy = event.y - cornerPt[1]
                    if (dx * dx + dy * dy <= touchRadius * touchRadius) {
                        deleteBoxIndex = i
                        break
                    }
                }
                if (deleteBoxIndex != -1) return true

                // Check resize handles on existing rectangles.
                val found = findResizeHandle(event.x, event.y, ivMatrix)
                if (found != null) {
                    resizeRegionIndex = found.first
                    resizeHandle = found.second
                    parent?.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                    return true
                }

                // Otherwise we'll start drawing a new box on MOVE.
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Delete marker drag-cancel check.
                if (deleteBoxIndex != -1) {
                    val dxDrag = event.x - startScreenX
                    val dyDrag = event.y - startScreenY
                    if (dxDrag * dxDrag + dyDrag * dyDrag > (8f * density) * (8f * density)) {
                        deleteBoxIndex = -1
                    } else {
                        return true
                    }
                }

                // Resize an existing rectangle.
                if (resizeRegionIndex != -1 && resizeHandle != null) {
                    moveResizeHandle(resizeRegionIndex, resizeHandle!!, x, y)
                    invalidate()
                    return true
                }

                // Draw a new rectangle.
                var activeBox = currentActiveBox
                if (activeBox == null) {
                    activeBox = RectF(startX, startY, startX, startY)
                    currentActiveBox = activeBox
                }
                activeBox.left = min(startX, x)
                activeBox.top = min(startY, y)
                activeBox.right = max(startX, x)
                activeBox.bottom = max(startY, y)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                // Delete marker tap.
                if (deleteBoxIndex != -1) {
                    if (deleteBoxIndex < regions.size) {
                        regions.removeAt(deleteBoxIndex)
                    }
                    deleteBoxIndex = -1
                    invalidate()
                    return true
                }

                // End resize.
                if (resizeRegionIndex != -1) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    resizeRegionIndex = -1
                    resizeHandle = null
                    invalidate()
                    return true
                }

                // End drawing.
                currentActiveBox?.let { activeBox ->
                    if (activeBox.width() > 15 || activeBox.height() > 15) {
                        regions.add(activeBox)
                    }
                    currentActiveBox = null
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                deleteBoxIndex = -1
                currentActiveBox = null
                resizeRegionIndex = -1
                resizeHandle = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Returns the (regionIndex, Handle) pair for the first handle that is
     * close enough to the touch point [screenX],[screenY], or null.
     *
     * Corners are checked before edges so that they take priority when the
     * rectangle is very small.
     */
    private fun findResizeHandle(
        screenX: Float,
        screenY: Float,
        ivMatrix: Matrix
    ): Pair<Int, Handle>? {
        val cornerSlop = cornerTouchSlopDp * density
        val edgeSlop = edgeTouchSlopDp * density
        val pts = FloatArray(2)

        for (i in regions.indices) {
            val r = regions[i]
            if (r.width() <= 0 || r.height() <= 0) continue

            // Corners.
            val corners = listOf(
                Handle.TOP_LEFT to floatArrayOf(r.left, r.top),
                Handle.TOP_RIGHT to floatArrayOf(r.right, r.top),
                Handle.BOTTOM_LEFT to floatArrayOf(r.left, r.bottom),
                Handle.BOTTOM_RIGHT to floatArrayOf(r.right, r.bottom)
            )
            for ((handle, imgPt) in corners) {
                pts[0] = imgPt[0]; pts[1] = imgPt[1]
                ivMatrix.mapPoints(pts)
                if (isNearScreen(screenX, screenY, pts[0], pts[1], cornerSlop)) {
                    return i to handle
                }
            }

            // Edges (midpoints).
            val edges = listOf(
                Handle.TOP to floatArrayOf(r.centerX(), r.top),
                Handle.BOTTOM to floatArrayOf(r.centerX(), r.bottom),
                Handle.LEFT to floatArrayOf(r.left, r.centerY()),
                Handle.RIGHT to floatArrayOf(r.right, r.centerY())
            )
            for ((handle, imgPt) in edges) {
                pts[0] = imgPt[0]; pts[1] = imgPt[1]
                ivMatrix.mapPoints(pts)
                if (isNearScreen(screenX, screenY, pts[0], pts[1], edgeSlop)) {
                    return i to handle
                }
            }
        }
        return null
    }

    private fun isNearScreen(
        x: Float, y: Float,
        targetX: Float, targetY: Float,
        slop: Float
    ): Boolean {
        val dx = x - targetX
        val dy = y - targetY
        return dx * dx + dy * dy <= slop * slop
    }

    /**
     * Moves the specified [handle] of region at [index] so that the
     * controlled edge/corner moves toward ([imgX], [imgY]) in image space.
     * A minimum size is enforced.
     */
    private fun moveResizeHandle(index: Int, handle: Handle, imgX: Float, imgY: Float) {
        val r = regions[index]
        val minSize = minRegionSizeDp

        when (handle) {
            Handle.TOP_LEFT -> {
                r.left = min(imgX, r.right - minSize)
                r.top = min(imgY, r.bottom - minSize)
            }

            Handle.TOP_RIGHT -> {
                r.right = max(imgX, r.left + minSize)
                r.top = min(imgY, r.bottom - minSize)
            }

            Handle.BOTTOM_LEFT -> {
                r.left = min(imgX, r.right - minSize)
                r.bottom = max(imgY, r.top + minSize)
            }

            Handle.BOTTOM_RIGHT -> {
                r.right = max(imgX, r.left + minSize)
                r.bottom = max(imgY, r.top + minSize)
            }

            Handle.TOP -> {
                r.top = min(imgY, r.bottom - minSize)
            }

            Handle.BOTTOM -> {
                r.bottom = max(imgY, r.top + minSize)
            }

            Handle.LEFT -> {
                r.left = min(imgX, r.right - minSize)
            }

            Handle.RIGHT -> {
                r.right = max(imgX, r.left + minSize)
            }
        }
    }


    fun resetZoom() {
        val iv = imageView ?: return
        iv.imageMatrix = Matrix(baseMatrix)
        invalidate()
    }

    /**
     * Clears all the blur regions.
     * */
    fun clearRegions() {
        regions.clear()
        invalidate()
    }

    /**
     * Returns blur regions scaled to the full-size original image dimensions.
     */
    fun getMappedBlurRegions(
        imageView: ImageView,
        originalWidth: Int,
        originalHeight: Int
    ): List<BlurRegion> {
        val mappedRegions = ArrayList<BlurRegion>()
        val drawable = imageView.drawable ?: return mappedRegions

        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight

        val scaleX = originalWidth.toFloat() / imageWidth
        val scaleY = originalHeight.toFloat() / imageHeight

        for (rect in regions) {
            val fullLeft = rect.left * scaleX
            val fullTop = rect.top * scaleY
            val fullWidth = rect.width() * scaleX
            val fullHeight = rect.height() * scaleY

            val cornerX = max(0, fullLeft.toInt())
            val cornerY = max(0, fullTop.toInt())
            val w = min(originalWidth - cornerX, fullWidth.toInt())
            val h = min(originalHeight - cornerY, fullHeight.toInt())

            if (w > 0 && h > 0) {
                mappedRegions.add(
                    BlurRegion(
                        w, h, cornerX, cornerY,
                        100, 100, true
                    )
                )
            }
        }
        return mappedRegions
    }
}
