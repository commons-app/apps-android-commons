package fr.free.nrw.commons.media.zoomControllers.gestures

import android.view.MotionEvent

/**
 * Component that detects translation, scale and rotation based on touch events.
 *
 * This class notifies its listeners whenever a gesture begins, updates or ends. Listeners read
 * pivot, translation, scale and rotation from [transform].
 */
class TransformGestureDetector(
    private val mDetector: MultiPointerGestureDetector,
) : MultiPointerGestureDetector.Listener {
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
        fun newInstance(): TransformGestureDetector =
            TransformGestureDetector(MultiPointerGestureDetector.newInstance())
    }

    /** Pivot, translation, scale and rotation for the current pointer positions. */
    val transform: GestureTransform
        get() = GestureTransform(mDetector.pointerSlots)

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
    fun onTouchEvent(event: MotionEvent): Boolean = mDetector.onTouchEvent(event)

    override fun onGestureBegin(detector: MultiPointerGestureDetector) {
        mListener?.onGestureBegin(this)
    }

    override fun onGestureUpdate(detector: MultiPointerGestureDetector) {
        mListener?.onGestureUpdate(this)
    }

    override fun onGestureEnd(detector: MultiPointerGestureDetector) {
        mListener?.onGestureEnd(this)
    }

    /** Restarts the current gesture (if any). */
    fun restartGesture() {
        mDetector.restartGesture()
    }
}
