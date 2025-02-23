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

    companion object {
        private const val MAX_POINTERS = 2

        /** Factory method that creates a new instance of MultiPointerGestureDetector */
        fun newInstance(): MultiPointerGestureDetector {
            return MultiPointerGestureDetector()
        }
    }

    private var mGestureInProgress = false
    private var mPointerCount = 0
    private var mNewPointerCount = 0
    private val mId = IntArray(MAX_POINTERS) { MotionEvent.INVALID_POINTER_ID }
    private val mStartX = FloatArray(MAX_POINTERS)
    private val mStartY = FloatArray(MAX_POINTERS)
    private val mCurrentX = FloatArray(MAX_POINTERS)
    private val mCurrentY = FloatArray(MAX_POINTERS)

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
        mPointerCount = 0
        for (i in 0 until MAX_POINTERS) {
            mId[i] = MotionEvent.INVALID_POINTER_ID
        }
    }

    /**
     * This method can be overridden in order to perform threshold check or something similar.
     *
     * @return whether or not to start a new gesture
     */
    protected open fun shouldStartGesture(): Boolean {
        return true
    }

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
     * Gets the index of the i-th pressed pointer. Normally, the index will be equal to i, except in
     * the case when the pointer is released.
     *
     * @return index of the specified pointer or -1 if not found (i.e. not enough pointers are down)
     */
    private fun getPressedPointerIndex(event: MotionEvent, i: Int): Int {
        val count = event.pointerCount
        val action = event.actionMasked
        val index = event.actionIndex
        var adjustedIndex = i

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (adjustedIndex >= index) {
                adjustedIndex++
            }
        }
        return if (adjustedIndex < count) adjustedIndex else -1
    }

    /** Gets the number of pressed pointers (fingers down). */
    private fun getPressedPointerCount(event: MotionEvent): Int {
        var count = event.pointerCount
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            count--
        }
        return count
    }

    private fun updatePointersOnTap(event: MotionEvent) {
        mPointerCount = 0
        for (i in 0 until MAX_POINTERS) {
            val index = getPressedPointerIndex(event, i)
            if (index == -1) {
                mId[i] = MotionEvent.INVALID_POINTER_ID
            } else {
                mId[i] = event.getPointerId(index)
                mCurrentX[i] = event.getX(index)
                mStartX[i] = mCurrentX[i]
                mCurrentY[i] = event.getY(index)
                mStartY[i] = mCurrentY[i]
                mPointerCount++
            }
        }
    }

    private fun updatePointersOnMove(event: MotionEvent) {
        for (i in 0 until MAX_POINTERS) {
            val index = event.findPointerIndex(mId[i])
            if (index != -1) {
                mCurrentX[i] = event.getX(index)
                mCurrentY[i] = event.getY(index)
            }
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
                updatePointersOnMove(event)
                // start a new gesture if not already started
                if (!mGestureInProgress && mPointerCount > 0 && shouldStartGesture()) {
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
            MotionEvent.ACTION_UP -> {
                // restart gesture whenever the number of pointers changes
                mNewPointerCount = getPressedPointerCount(event)
                stopGesture()
                updatePointersOnTap(event)
                if (mPointerCount > 0 && shouldStartGesture()) {
                    startGesture()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                mNewPointerCount = 0
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
        for (i in 0 until MAX_POINTERS) {
            mStartX[i] = mCurrentX[i]
            mStartY[i] = mCurrentY[i]
        }
        startGesture()
    }

    /** Gets whether there is a gesture in progress */
    fun isGestureInProgress(): Boolean {
        return mGestureInProgress
    }

    /** Gets the number of pointers after the current gesture */
    fun getNewPointerCount(): Int {
        return mNewPointerCount
    }

    /** Gets the number of pointers in the current gesture */
    fun getPointerCount(): Int {
        return mPointerCount
    }

    /**
     * Gets the start X coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getStartX(): FloatArray {
        return mStartX
    }

    /**
     * Gets the start Y coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getStartY(): FloatArray {
        return mStartY
    }

    /**
     * Gets the current X coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getCurrentX(): FloatArray {
        return mCurrentX
    }

    /**
     * Gets the current Y coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getCurrentY(): FloatArray {
        return mCurrentY
    }
}
