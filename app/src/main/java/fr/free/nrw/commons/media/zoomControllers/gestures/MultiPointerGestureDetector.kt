package fr.free.nrw.commons.media.zoomControllers.gestures

import android.view.MotionEvent

/**
 * Component that detects and tracks multiple pointers based on touch events.
 *
 * Each time a pointer gets pressed or released, the current gesture (if any) will end, and a new
 * one will be started (if there are still pressed pointers left). It is guaranteed that the number
 * of pointers within the single gesture will remain the same during the whole gesture.
 */
open class MultiPointerGestureDetector {
    /** The listener for receiving notifications when gestures occur. */
    interface Listener {
        /** A callback called right before the gesture is about to start. */
        fun onGestureBegin(detector: MultiPointerGestureDetector)

        /** A callback called each time the gesture gets updated. */
        fun onGestureUpdate(detector: MultiPointerGestureDetector)

        /** A callback called right after the gesture has finished. */
        fun onGestureEnd(detector: MultiPointerGestureDetector)
    }

    /** Factory method that creates a new instance of MultiPointerGestureDetector */
    companion object {
        fun newInstance(): MultiPointerGestureDetector = MultiPointerGestureDetector()
    }

    /** Pointer ids and coordinates for the fingers currently down. */
    val pointerSlots: PointerSlots = PointerSlots()

    private var mGestureInProgress = false

    private var mListener: Listener? = null

    init {
        reset()
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
        mGestureInProgress = false
        pointerSlots.reset()
    }

    /**
     * This method can be overridden in order to perform threshold check or something similar.
     *
     * @return whether or not to start a new gesture
     */
    protected open fun shouldStartGesture(): Boolean = true

    /** Starts a new gesture and calls the listener just before starting it. */
    private fun startGesture() {
        if (!mGestureInProgress) {
            mListener?.onGestureBegin(this)
            mGestureInProgress = true
        }
    }

    /** Stops the current gesture and calls the listener right after stopping it. */
    private fun stopGesture() {
        if (mGestureInProgress) {
            mGestureInProgress = false
            mListener?.onGestureEnd(this)
        }
    }

    /**
     * Handles the given motion event.
     *
     * @param event event to handle
     * @return whether or not the event was handled
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                // update pointers
                pointerSlots.updatePointersOnMove(event)
                // start a new gesture if not already started
                if (!mGestureInProgress && pointerSlots.pointerCount > 0 && shouldStartGesture()) {
                    startGesture()
                }
                // notify listener
                if (mGestureInProgress) {
                    mListener?.onGestureUpdate(this)
                }
            }

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            -> {
                // restart gesture whenever the number of pointers changes
                pointerSlots.newPointerCount = pointerSlots.getPressedPointerCount(event)
                stopGesture()
                pointerSlots.updatePointersOnTap(event)
                if (pointerSlots.pointerCount > 0 && shouldStartGesture()) {
                    startGesture()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                pointerSlots.newPointerCount = 0
                stopGesture()
                reset()
            }
        }
        return true
    }

    /** Restarts the current gesture (if any). */
    fun restartGesture() {
        if (!mGestureInProgress) {
            return
        }
        stopGesture()
        pointerSlots.copyCurrentToStart()
        startGesture()
    }

    /** Gets whether there is a gesture in progress */
    fun isGestureInProgress(): Boolean = mGestureInProgress
}
