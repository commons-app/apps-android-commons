package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Class for detecting swipe gestures
 */
open class OnSwipeTouchListener(context: Context?) : View.OnTouchListener {

    private val gestureDetector: GestureDetector

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onFling(
            event1: MotionEvent,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                val diffY: Float = event2.y - event1.y
                val diffX: Float = event2.x - event1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) >
                        SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) >
                        SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeDown()
                        } else {
                            onSwipeUp()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }
}