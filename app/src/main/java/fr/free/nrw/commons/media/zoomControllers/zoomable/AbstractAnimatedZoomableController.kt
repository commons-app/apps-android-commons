package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.graphics.Matrix
import android.graphics.PointF
import com.facebook.common.logging.FLog
import fr.free.nrw.commons.media.zoomControllers.gestures.TransformGestureDetector
import fr.free.nrw.commons.media.zoomControllers.zoomable.TransformLimiter.LimitFlag

/**
 * Abstract class for ZoomableController that adds animation capabilities to
 * DefaultZoomableController.
 */
abstract class AbstractAnimatedZoomableController(
    transformGestureDetector: TransformGestureDetector,
) : DefaultZoomableController(transformGestureDetector) {
    protected val animation = AnimationState()

    override fun reset() {
        FLog.v(logTag, "reset")
        stopAnimation()
        animation.getNewTransform().reset()
        animation.getWorkingTransform().reset()
        super.reset()
    }

    /** Returns true if the zoomable transform is identity matrix, and the controller is idle. */
    override fun isIdentity(): Boolean = !animation.getIsAnimating() && super.isIdentity()

    /**
     * Zooms to the desired scale and positions the image so that the given image point corresponds
     * to the given view point.
     *
     * If this method is called while an animation or gesture is already in progress, the current
     * animation or gesture will be stopped first.
     *
     * @param scale desired scale, will be limited to {min, max} scale factor
     * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint 2D point in view's absolute coordinate system
     */
    override fun zoomToPoint(
        scale: Float,
        imagePoint: PointF,
        viewPoint: PointF,
    ) {
        zoomToPoint(scale, imagePoint, viewPoint, TransformLimiter.LIMIT_ALL, 0, null)
    }

    /**
     * Zooms to the desired scale and positions the image so that the given image point corresponds
     * to the given view point.
     *
     * If this method is called while an animation or gesture is already in progress, the current
     * animation or gesture will be stopped first.
     *
     * @param scale desired scale, will be limited to {min, max} scale factor
     * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint 2D point in view's absolute coordinate system
     * @param limitFlags whether to limit translation and/or scale.
     * @param durationMs length of animation of the zoom, or 0 if no animation desired
     * @param onAnimationComplete code to run when the animation completes. Ignored if durationMs=0
     */
    fun zoomToPoint(
        scale: Float,
        imagePoint: PointF,
        viewPoint: PointF,
        @LimitFlag limitFlags: Int,
        durationMs: Long,
        onAnimationComplete: Runnable?,
    ) {
        FLog.v(logTag, "zoomToPoint: duration $durationMs ms")
        calculateZoomToPointTransform(animation.getNewTransform(), scale, imagePoint, viewPoint, limitFlags)
        setTransform(animation.getNewTransform(), durationMs, onAnimationComplete)
    }

    /**
     * Sets a new zoomable transformation and animates to it if desired.
     *
     * If this method is called while an animation or gesture is already in progress, the current
     * animation or gesture will be stopped first.
     *
     * @param newTransform new transform to make active
     * @param durationMs duration of the animation, or 0 to not animate
     * @param onAnimationComplete code to run when the animation completes. Ignored if durationMs=0
     */
    private fun setTransform(
        newTransform: Matrix,
        durationMs: Long,
        onAnimationComplete: Runnable?,
    ) {
        FLog.v(logTag, "setTransform: duration $durationMs ms")
        if (durationMs <= 0) {
            setTransformImmediate(newTransform)
        } else {
            setTransformAnimated(newTransform, durationMs, onAnimationComplete)
        }
    }

    private fun setTransformImmediate(newTransform: Matrix) {
        FLog.v(logTag, "setTransformImmediate")
        stopAnimation()
        animation.getWorkingTransform().set(newTransform)
        super.setTransform(newTransform)
        getDetector().restartGesture()
    }

    override fun onGestureBegin(detector: TransformGestureDetector) {
        FLog.v(logTag, "onGestureBegin")
        stopAnimation()
        super.onGestureBegin(detector)
    }

    override fun onGestureUpdate(detector: TransformGestureDetector) {
        FLog.v(logTag, "onGestureUpdate ${if (animation.getIsAnimating()) "(ignored)" else ""}")
        if (animation.getIsAnimating()) return
        super.onGestureUpdate(detector)
    }

    abstract fun setTransformAnimated(
        newTransform: Matrix,
        durationMs: Long,
        onAnimationComplete: Runnable?,
    )

    protected abstract fun stopAnimation()

    protected abstract val logTag: Class<*>
}
