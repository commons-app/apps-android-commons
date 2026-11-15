package fr.free.nrw.commons.edit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

private typealias CropHandle = CropRectEditor.Handle

/**
 * Custom View that draws a resizable crop rectangle with handles.
 * Supports dragging corners/edges to resize and dragging center to move.
 */
class CropOverlayView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        // Density for converting dp to pixels
        private val density = context.resources.displayMetrics.density

        // Density-aware sizing
        private val handleRadius = 12f * density // 12dp for edge handles
        private val cornerHandleRadius = 16f * density // 16dp for corner handles (more prominent)
        private val minCropSize = 80f * density // 80dp minimum crop size
        private val touchSlop = 48f * density // 48dp touch area for edges
        private val cornerTouchSlop = 56f * density // 56dp larger touch area for corners
        private val borderWidth = 2f * density // 2dp border
        private val gridStrokeWidth = 1f * density // 1dp grid lines
        private val centerIndicatorSize = 16f * density // 16dp center crosshair

        private val cropRect = RectF()

        private val borderPaint =
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                isAntiAlias = true
            }

        private val handlePaint =
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }

        private val activeHandlePaint =
            Paint().apply {
                color = Color.parseColor("#2196F3") // Material Blue
                style = Paint.Style.FILL
                isAntiAlias = true
            }

        private val handleBorderPaint =
            Paint().apply {
                color = Color.parseColor("#424242") // Dark gray border for contrast
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                isAntiAlias = true
            }

        private val overlayPaint =
            Paint().apply {
                color = Color.argb(128, 0, 0, 0)
                style = Paint.Style.FILL
            }

        private val gridPaint =
            Paint().apply {
                color = Color.argb(180, 255, 255, 255) // Higher opacity for visibility
                style = Paint.Style.STROKE
                strokeWidth = gridStrokeWidth
                isAntiAlias = true
            }

        private val centerIndicatorPaint =
            Paint().apply {
                color = Color.argb(100, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                isAntiAlias = true
            }

        private var activeHandle: CropHandle? = null
        private var lastTouchX = 0f
        private var lastTouchY = 0f

        // Image bounds within this view (set by EditActivity)
        private var imageBounds = RectF()

        private val cropEditor =
            CropRectEditor(
                cropRect = cropRect,
                minCropSize = minCropSize,
                cornerTouchSlop = cornerTouchSlop,
                edgeTouchSlop = touchSlop,
            )

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (cropRect.isEmpty) {
                resetCropRect()
            }
        }

        /**
         * Sets the image bounds within this view.
         * The crop overlay will be constrained to these bounds.
         */
        fun setImageBounds(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
        ) {
            imageBounds.set(left, top, right, bottom)
            resetCropRect()
            invalidate()
        }

        /**
         * Resets the crop rectangle to cover the entire image bounds.
         */
        fun resetCropRect() {
            if (!imageBounds.isEmpty) {
                cropRect.set(imageBounds)
            } else {
                val padding = 50f * density
                cropRect.set(
                    padding,
                    padding,
                    width.toFloat() - padding,
                    height.toFloat() - padding,
                )
            }
            invalidate()
        }

        /**
         * Returns the crop rectangle in view coordinates.
         */
        fun getCropRect(): RectF = RectF(cropRect)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Draw dark overlay outside crop area
            canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
            canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
            canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
            canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

            // Draw crop border
            canvas.drawRect(cropRect, borderPaint)

            // Draw rule of thirds grid
            val thirdWidth = cropRect.width() / 3
            val thirdHeight = cropRect.height() / 3
            for (i in 1..2) {
                canvas.drawLine(
                    cropRect.left + thirdWidth * i,
                    cropRect.top,
                    cropRect.left + thirdWidth * i,
                    cropRect.bottom,
                    gridPaint,
                )
                canvas.drawLine(
                    cropRect.left,
                    cropRect.top + thirdHeight * i,
                    cropRect.right,
                    cropRect.top + thirdHeight * i,
                    gridPaint,
                )
            }

            // Draw center indicator (crosshair) for drag affordance
            drawCenterIndicator(canvas)

            // Draw corner handles (larger)
            drawCornerHandle(canvas, cropRect.left, cropRect.top, CropHandle.TOP_LEFT)
            drawCornerHandle(canvas, cropRect.right, cropRect.top, CropHandle.TOP_RIGHT)
            drawCornerHandle(canvas, cropRect.left, cropRect.bottom, CropHandle.BOTTOM_LEFT)
            drawCornerHandle(canvas, cropRect.right, cropRect.bottom, CropHandle.BOTTOM_RIGHT)

            // Draw edge handles (smaller)
            drawEdgeHandle(canvas, cropRect.centerX(), cropRect.top, CropHandle.TOP)
            drawEdgeHandle(canvas, cropRect.centerX(), cropRect.bottom, CropHandle.BOTTOM)
            drawEdgeHandle(canvas, cropRect.left, cropRect.centerY(), CropHandle.LEFT)
            drawEdgeHandle(canvas, cropRect.right, cropRect.centerY(), CropHandle.RIGHT)

            // Update gesture exclusion zones for edge handles near screen edges
            updateGestureExclusion()
        }

        private fun drawCornerHandle(
            canvas: Canvas,
            x: Float,
            y: Float,
            handle: CropHandle,
        ) {
            val isActive = activeHandle == handle
            val paint = if (isActive) activeHandlePaint else handlePaint

            // Draw filled circle
            canvas.drawCircle(x, y, cornerHandleRadius, paint)
            // Draw border for visibility against various backgrounds
            canvas.drawCircle(x, y, cornerHandleRadius, handleBorderPaint)
        }

        private fun drawEdgeHandle(
            canvas: Canvas,
            x: Float,
            y: Float,
            handle: CropHandle,
        ) {
            val isActive = activeHandle == handle
            val paint = if (isActive) activeHandlePaint else handlePaint

            // Draw filled circle
            canvas.drawCircle(x, y, handleRadius, paint)
            // Draw border for visibility
            canvas.drawCircle(x, y, handleRadius, handleBorderPaint)
        }

        private fun drawCenterIndicator(canvas: Canvas) {
            val centerX = cropRect.centerX()
            val centerY = cropRect.centerY()

            // Draw subtle crosshair to indicate the area can be moved
            canvas.drawLine(
                centerX - centerIndicatorSize,
                centerY,
                centerX + centerIndicatorSize,
                centerY,
                centerIndicatorPaint,
            )
            canvas.drawLine(
                centerX,
                centerY - centerIndicatorSize,
                centerX,
                centerY + centerIndicatorSize,
                centerIndicatorPaint,
            )
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    activeHandle = cropEditor.findHandle(x, y)
                    if (activeHandle != null) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        lastTouchX = x
                        lastTouchY = y
                        invalidate() // Redraw to show active handle state
                        return true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (activeHandle != null) {
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        val bounds =
                            if (!imageBounds.isEmpty) {
                                imageBounds
                            } else {
                                RectF(0f, 0f, width.toFloat(), height.toFloat())
                            }
                        cropEditor.moveHandle(activeHandle!!, dx, dy, bounds)
                        lastTouchX = x
                        lastTouchY = y
                        invalidate()
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (activeHandle != null) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        activeHandle = null
                        invalidate() // Redraw to clear active handle state
                    }
                }
            }
            return super.onTouchEvent(event)
        }

        private fun updateGestureExclusion() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val exclusionRects = mutableListOf<Rect>()
                val handleExclusion = (48 * density).toInt() // 48dp per handle rect
                val handleSize = cornerTouchSlop.toInt()

                // Left edge: add small rects around each handle (top-left, left-mid, bottom-left)
                if (cropRect.left < handleSize) {
                    val leftX = 0
                    val rightX = handleSize
                    for (cy in listOf(cropRect.top, cropRect.centerY(), cropRect.bottom)) {
                        exclusionRects.add(
                            Rect(
                                leftX,
                                (cy - handleExclusion / 2).toInt(),
                                rightX,
                                (cy + handleExclusion / 2).toInt(),
                            ),
                        )
                    }
                }

                // Right edge: add small rects around each handle (top-right, right-mid, bottom-right)
                if (width - cropRect.right < handleSize) {
                    val leftX = width - handleSize
                    val rightX = width
                    for (cy in listOf(cropRect.top, cropRect.centerY(), cropRect.bottom)) {
                        exclusionRects.add(
                            Rect(
                                leftX,
                                (cy - handleExclusion / 2).toInt(),
                                rightX,
                                (cy + handleExclusion / 2).toInt(),
                            ),
                        )
                    }
                }

                systemGestureExclusionRects = exclusionRects
            }
        }
    }
