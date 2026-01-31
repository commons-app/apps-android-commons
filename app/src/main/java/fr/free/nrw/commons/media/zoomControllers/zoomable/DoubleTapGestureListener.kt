package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Tap gesture listener for double tap to zoom/unzoom and double-tap-and-drag to zoom.
 *
 * @see ZoomableDraweeView.setTapListener
 */
class DoubleTapGestureListener(
    private val draweeView: ZoomableDraweeView,
    private val onSingleTap: (() -> Unit)? = null
) : GestureDetector.SimpleOnGestureListener() {

    companion object {
        private const val DURATION_MS = 300L
        private const val DOUBLE_TAP_SCROLL_THRESHOLD = 20
    }

    private val doubleTapViewPoint = PointF()
    private val doubleTapImagePoint = PointF()
    private var doubleTapScale = 1f
    private var doubleTapScroll = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if(onSingleTap != null) {
            onSingleTap.invoke()
            return true
        } else {
            return super.onSingleTapConfirmed(e)
        }
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        val zc = draweeView.getZoomableController() as AbstractAnimatedZoomableController
        val vp = PointF(e.x, e.y)
        val ip = zc.mapViewToImage(vp)

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                doubleTapViewPoint.set(vp)
                doubleTapImagePoint.set(ip)
                doubleTapScale = zc.getScaleFactor()
            }

            MotionEvent.ACTION_MOVE -> {
                doubleTapScroll = doubleTapScroll || shouldStartDoubleTapScroll(vp)
                if (doubleTapScroll) {
                    val scale = calcScale(vp)
                    zc.zoomToPoint(scale, doubleTapImagePoint, doubleTapViewPoint)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (doubleTapScroll) {
                    val scale = calcScale(vp)
                    zc.zoomToPoint(scale, doubleTapImagePoint, doubleTapViewPoint)
                } else {
                    val maxScale = zc.getMaxScaleFactor()
                    val minScale = zc.getMinScaleFactor()
                    val targetScale =
                        if (zc.getScaleFactor() < (maxScale + minScale) / 2) maxScale else minScale

                    zc.zoomToPoint(
                        targetScale,
                        ip,
                        vp,
                        DefaultZoomableController.LIMIT_ALL,
                        DURATION_MS,
                        null
                    )
                }
                doubleTapScroll = false
            }
        }
        return true
    }

    private fun shouldStartDoubleTapScroll(viewPoint: PointF): Boolean {
        val dist = hypot(
            (viewPoint.x - doubleTapViewPoint.x).toDouble(),
            (viewPoint.y - doubleTapViewPoint.y).toDouble()
        )
        return dist > DOUBLE_TAP_SCROLL_THRESHOLD
    }

    private fun calcScale(currentViewPoint: PointF): Float {
        val dy = currentViewPoint.y - doubleTapViewPoint.y
        val t = 1 + abs(dy) * 0.001f
        return if (dy < 0) doubleTapScale / t else doubleTapScale * t
    }
}
