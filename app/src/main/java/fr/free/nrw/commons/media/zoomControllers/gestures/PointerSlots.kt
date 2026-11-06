package fr.free.nrw.commons.media.zoomControllers.gestures

import android.view.MotionEvent

/**
 * Pointer ids and coordinates for the fingers in one gesture.
 *
 * At most two pointers are tracked. The coordinate arrays are exposed for performance and must not
 * be modified by callers.
 */
class PointerSlots {
    private val mId = IntArray(MAX_POINTERS) { MotionEvent.INVALID_POINTER_ID }
    private val mStartX = FloatArray(MAX_POINTERS)
    private val mStartY = FloatArray(MAX_POINTERS)
    private val mCurrentX = FloatArray(MAX_POINTERS)
    private val mCurrentY = FloatArray(MAX_POINTERS)

    /** Gets the number of pointers in the current gesture */
    var pointerCount: Int = 0
        private set

    /** Gets the number of pointers after the current gesture */
    var newPointerCount: Int = 0

    /** Gets the number of pressed pointers (fingers down). */
    fun getPressedPointerCount(event: MotionEvent): Int {
        var count = event.pointerCount
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            count--
        }
        return count
    }

    /** Clears the tracked pointers. */
    fun reset() {
        pointerCount = 0
        for (i in 0 until MAX_POINTERS) {
            mId[i] = MotionEvent.INVALID_POINTER_ID
        }
    }

    fun updatePointersOnTap(event: MotionEvent) {
        /**
         * Gets the index of the i-th pressed pointer. Normally, the index will be equal to i, except in
         * the case when the pointer is released.
         *
         * @return index of the specified pointer or -1 if not found (i.e. not enough pointers are down)
         */
        fun pressedPointerIndex(i: Int): Int {
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

        pointerCount = 0
        for (i in 0 until MAX_POINTERS) {
            val index = pressedPointerIndex(i)
            if (index == -1) {
                mId[i] = MotionEvent.INVALID_POINTER_ID
            } else {
                mId[i] = event.getPointerId(index)
                mCurrentX[i] = event.getX(index)
                mStartX[i] = mCurrentX[i]
                mCurrentY[i] = event.getY(index)
                mStartY[i] = mCurrentY[i]
                pointerCount++
            }
        }
    }

    fun updatePointersOnMove(event: MotionEvent) {
        for (i in 0 until MAX_POINTERS) {
            val index = event.findPointerIndex(mId[i])
            if (index != -1) {
                mCurrentX[i] = event.getX(index)
                mCurrentY[i] = event.getY(index)
            }
        }
    }

    /** Stores the current coordinates as the start of a restarted gesture. */
    fun copyCurrentToStart() {
        for (i in 0 until MAX_POINTERS) {
            mStartX[i] = mCurrentX[i]
            mStartY[i] = mCurrentY[i]
        }
    }

    /**
     * Gets the start X coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getStartX(): FloatArray = mStartX

    /**
     * Gets the start Y coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getStartY(): FloatArray = mStartY

    /**
     * Gets the current X coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getCurrentX(): FloatArray = mCurrentX

    /**
     * Gets the current Y coordinates for all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    fun getCurrentY(): FloatArray = mCurrentY

    private companion object {
        const val MAX_POINTERS = 2
    }
}
