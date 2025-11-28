package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/**
 * Class for detecting swipe gestures
 */
open class OnSwipeTouchListener(
    context: Context?,
) : View.OnTouchListener {
    private val gestureDetector: GestureDetector

    private val swipeThresholdHeight = (getScreenResolution(context!!)).second / 3
    private val swipeThresholdWidth = (getScreenResolution(context!!)).first / 3
    private val swipeVelocityThreshold = 1000

    override fun onTouch(
        view: View?,
        motionEvent: MotionEvent,
    ): Boolean = gestureDetector.onTouchEvent(motionEvent)

    fun getScreenResolution(context: Context): Pair<Int, Int> {
        val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.getDefaultDisplay()
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val width: Int = metrics.widthPixels
        val height: Int = metrics.heightPixels
        return width to height
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = false

        /**
         * Detects the gestures
         */
        override fun onFling(
            event1: MotionEvent?,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            try {
                val diffY: Float = event2.y - (event1?.y ?: event2.y)
                val diffX: Float = event2.x - (event1?.x ?: event2.x)
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThresholdWidth &&
                        abs(velocityX) >
                        swipeVelocityThreshold
                    ) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else {
                    if (abs(diffY) > swipeThresholdHeight &&
                        abs(velocityY) >
                        swipeVelocityThreshold
                    ) {
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

    /**
     * Swipe right to view previous image
     */
    open fun onSwipeRight() {}

    /**
     * Swipe left to view next image
     */
    open fun onSwipeLeft() {}

    /**
     * Swipe up to select the picture (the equivalent of tapping it in non-fullscreen mode)
     * and show the next picture skipping pictures that have either already been uploaded or
     * marked as not for upload
     */
    open fun onSwipeUp() {}

    /**
     * Swipe down to mark that picture as "Not for upload" (the equivalent of selecting it then
     * tapping "Mark as not for upload" in non-fullscreen mode), and show the next picture.
     */
    open fun onSwipeDown() {}

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }
}
