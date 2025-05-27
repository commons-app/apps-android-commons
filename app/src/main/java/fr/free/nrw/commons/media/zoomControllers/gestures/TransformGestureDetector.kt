package fr.free.nrw.commons.media.zoomControllers.gestures

import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Component that detects translation, scale and rotation based on touch events.
 *
 * This class notifies its listeners whenever a gesture begins, updates or ends. The instance of
 * this detector is passed to the listeners, so it can be queried for pivot, translation, scale or
 * rotation.
 */
class TransformGestureDetector(private val mDetector: MultiPointerGestureDetector) :
    MultiPointerGestureDetector.Listener {

    /** The listener for receiving notifications when gestures occur. */
    interface Listener {
        /** A callback called right before the gesture is about to start. */
        fun onGestureBegin(detector: TransformGestureDetector)

        /** A callback called each time the gesture gets updated. */
        fun onGestureUpdate(detector: TransformGestureDetector)

        /** A callback called right after the gesture has finished. */
        fun onGestureEnd(detector: TransformGestureDetector)
    }

    private var mListener: Listener? = null

    init {
        mDetector.setListener(this)
    }

    /** Factory method that creates a new instance of TransformGestureDetector */
    companion object {
        fun newInstance(): TransformGestureDetector {
            return TransformGestureDetector(MultiPointerGestureDetector.newInstance())
        }
    }

    /**
     * Sets the listener.
     *
     * @param listener listener to set
     */
    fun setListener(listener: Listener?) {
        mListener = listener
    }

    /** Resets the component to the initial state. */
    fun reset() {
        mDetector.reset()
    }

    /**
     * Handles the given motion event.
     *
     * @param event event to handle
     * @return whether or not the event was handled
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return mDetector.onTouchEvent(event)
    }

    override fun onGestureBegin(detector: MultiPointerGestureDetector) {
        mListener?.onGestureBegin(this)
    }

    override fun onGestureUpdate(detector: MultiPointerGestureDetector) {
        mListener?.onGestureUpdate(this)
    }

    override fun onGestureEnd(detector: MultiPointerGestureDetector) {
        mListener?.onGestureEnd(this)
    }

    private fun calcAverage(arr: FloatArray, len: Int): Float {
        val sum = arr.take(len).sum()
        return if (len > 0) sum / len else 0f
    }

    /** Restarts the current gesture (if any). */
    fun restartGesture() {
        mDetector.restartGesture()
    }

    /** Gets whether there is a gesture in progress */
    fun isGestureInProgress(): Boolean {
        return mDetector.isGestureInProgress()
    }

    /** Gets the number of pointers after the current gesture */
    fun getNewPointerCount(): Int {
        return mDetector.getNewPointerCount()
    }

    /** Gets the number of pointers in the current gesture */
    fun getPointerCount(): Int {
        return mDetector.getPointerCount()
    }

    /** Gets the X coordinate of the pivot point */
    fun getPivotX(): Float {
        return calcAverage(mDetector.getStartX(), mDetector.getPointerCount())
    }

    /** Gets the Y coordinate of the pivot point */
    fun getPivotY(): Float {
        return calcAverage(mDetector.getStartY(), mDetector.getPointerCount())
    }

    /** Gets the X component of the translation */
    fun getTranslationX(): Float {
        return calcAverage(mDetector.getCurrentX(), mDetector.getPointerCount()) -
                calcAverage(mDetector.getStartX(), mDetector.getPointerCount())
    }

    /** Gets the Y component of the translation */
    fun getTranslationY(): Float {
        return calcAverage(mDetector.getCurrentY(), mDetector.getPointerCount()) -
                calcAverage(mDetector.getStartY(), mDetector.getPointerCount())
    }

    /** Gets the scale */
    fun getScale(): Float {
        return if (mDetector.getPointerCount() < 2) {
            1f
        } else {
            val startDeltaX = mDetector.getStartX()[1] - mDetector.getStartX()[0]
            val startDeltaY = mDetector.getStartY()[1] - mDetector.getStartY()[0]
            val currentDeltaX = mDetector.getCurrentX()[1] - mDetector.getCurrentX()[0]
            val currentDeltaY = mDetector.getCurrentY()[1] - mDetector.getCurrentY()[0]
            val startDist = hypot(startDeltaX, startDeltaY)
            val currentDist = hypot(currentDeltaX, currentDeltaY)
            currentDist / startDist
        }
    }

    /** Gets the rotation in radians */
    fun getRotation(): Float {
        return if (mDetector.getPointerCount() < 2) {
            0f
        } else {
            val startDeltaX = mDetector.getStartX()[1] - mDetector.getStartX()[0]
            val startDeltaY = mDetector.getStartY()[1] - mDetector.getStartY()[0]
            val currentDeltaX = mDetector.getCurrentX()[1] - mDetector.getCurrentX()[0]
            val currentDeltaY = mDetector.getCurrentY()[1] - mDetector.getCurrentY()[0]
            val startAngle = atan2(startDeltaY, startDeltaX)
            val currentAngle = atan2(currentDeltaY, currentDeltaX)
            currentAngle - startAngle
        }
    }
}
