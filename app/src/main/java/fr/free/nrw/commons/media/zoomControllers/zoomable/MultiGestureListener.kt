package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.RequiresApi

/**
 * Gesture listener that allows multiple child listeners to be added and notified about gesture
 * events.
 *
 * NOTE: The order of the listeners is important. Listeners can consume gesture events. For
 * example, if one of the child listeners consumes [onLongPress] (the listener returned true),
 * subsequent listeners will not be notified about the event anymore since it has been consumed.
 */
class MultiGestureListener : GestureDetector.SimpleOnGestureListener() {
    /** Child listeners notified in the order they were added. */
    val childListeners: GestureListenerList = GestureListenerList()

    @Synchronized
    override fun onSingleTapUp(e: MotionEvent): Boolean = childListeners.firstConsuming { it.onSingleTapUp(e) }

    @Synchronized
    override fun onLongPress(e: MotionEvent) {
        childListeners.notifyEach { it.onLongPress(e) }
    }

    @Synchronized
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean = childListeners.firstConsuming { it.onScroll(e1, e2, distanceX, distanceY) }

    @Synchronized
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean = childListeners.firstConsuming { it.onFling(e1, e2, velocityX, velocityY) }

    @Synchronized
    override fun onShowPress(e: MotionEvent) {
        childListeners.notifyEach { it.onShowPress(e) }
    }

    @Synchronized
    override fun onDown(e: MotionEvent): Boolean = childListeners.firstConsuming { it.onDown(e) }

    @Synchronized
    override fun onDoubleTap(e: MotionEvent): Boolean = childListeners.firstConsuming { it.onDoubleTap(e) }

    @Synchronized
    override fun onDoubleTapEvent(e: MotionEvent): Boolean = childListeners.firstConsuming { it.onDoubleTapEvent(e) }

    @Synchronized
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean =
        childListeners.firstConsuming { it.onSingleTapConfirmed(e) }

    @RequiresApi(Build.VERSION_CODES.M)
    @Synchronized
    override fun onContextClick(e: MotionEvent): Boolean = childListeners.firstConsuming { it.onContextClick(e) }
}
