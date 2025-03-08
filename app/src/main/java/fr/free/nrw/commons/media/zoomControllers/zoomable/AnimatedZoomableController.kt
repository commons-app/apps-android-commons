package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.view.animation.DecelerateInterpolator
import com.facebook.common.logging.FLog
import fr.free.nrw.commons.media.zoomControllers.gestures.TransformGestureDetector

/**
 * ZoomableController that adds animation capabilities to DefaultZoomableController using standard
 * Android animation classes
 */
class AnimatedZoomableController private constructor() :
    AbstractAnimatedZoomableController(TransformGestureDetector.newInstance()) {

    private val valueAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        interpolator = DecelerateInterpolator()
    }

    companion object {
        fun newInstance(): AnimatedZoomableController {
            return AnimatedZoomableController()
        }
    }

    @SuppressLint("NewApi")
    override fun setTransformAnimated(
        newTransform: Matrix, durationMs: Long, onAnimationComplete: Runnable?
    ) {
        FLog.v(logTag, "setTransformAnimated: duration $durationMs ms")
        stopAnimation()
        require(durationMs > 0) { "Duration must be greater than zero" }
        check(!getIsAnimating()) { "Animation is already in progress" }
        setAnimating(true)
        valueAnimator.duration = durationMs
        getTransform().getValues(getStartValues())
        newTransform.getValues(getStopValues())
        valueAnimator.addUpdateListener { animator ->
            calculateInterpolation(getWorkingTransform(), animator.animatedValue as Float)
            super.setTransform(getWorkingTransform())
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                FLog.v(logTag, "setTransformAnimated: animation cancelled")
                onAnimationStopped()
            }

            override fun onAnimationEnd(animation: Animator) {
                FLog.v(logTag, "setTransformAnimated: animation finished")
                onAnimationStopped()
            }

            private fun onAnimationStopped() {
                onAnimationComplete?.run()
                setAnimating(false)
                getDetector().restartGesture()
            }
        })
        valueAnimator.start()
    }

    @SuppressLint("NewApi")
    override fun stopAnimation() {
        if (!getIsAnimating()) return
        FLog.v(logTag, "stopAnimation")
        valueAnimator.cancel()
        valueAnimator.removeAllUpdateListeners()
        valueAnimator.removeAllListeners()
    }

    override val logTag: Class<*> = AnimatedZoomableController::class.java
}
