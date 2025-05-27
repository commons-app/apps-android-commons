package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import java.util.Collections.synchronizedList

/**
 * Gesture listener that allows multiple child listeners to be added and notified about gesture
 * events.
 *
 * NOTE: The order of the listeners is important. Listeners can consume gesture events. For
 * example, if one of the child listeners consumes [onLongPress] (the listener returned true),
 * subsequent listeners will not be notified about the event anymore since it has been consumed.
 */
class MultiGestureListener : GestureDetector.SimpleOnGestureListener() {

    private val listeners: MutableList<GestureDetector.SimpleOnGestureListener> =
        synchronizedList(mutableListOf())

    /**
     * Adds a listener to the multi-gesture listener.
     *
     * NOTE: The order of the listeners is important since gesture events can be consumed.
     *
     * @param listener the listener to be added
     */
    @Synchronized
    fun addListener(listener: GestureDetector.SimpleOnGestureListener) {
        listeners.add(listener)
    }

    /**
     * Removes the given listener so that it will not be notified about future events.
     *
     * NOTE: The order of the listeners is important since gesture events can be consumed.
     *
     * @param listener the listener to remove
     */
    @Synchronized
    fun removeListener(listener: GestureDetector.SimpleOnGestureListener) {
        listeners.remove(listener)
    }

    @Synchronized
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        for (listener in listeners) {
            if (listener.onSingleTapUp(e)) {
                return true
            }
        }
        return false
    }

    @Synchronized
    override fun onLongPress(e: MotionEvent) {
        for (listener in listeners) {
            listener.onLongPress(e)
        }
    }

    @Synchronized
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        for (listener in listeners) {
            if (listener.onScroll(e1, e2, distanceX, distanceY)) {
                return true
            }
        }
        return false
    }

    @Synchronized
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        for (listener in listeners) {
            if (listener.onFling(e1, e2, velocityX, velocityY)) {
                return true
            }
        }
        return false
    }

    @Synchronized
    override fun onShowPress(e: MotionEvent) {
        for (listener in listeners) {
            listener.onShowPress(e)
        }
    }

    @Synchronized
    override fun onDown(e: MotionEvent): Boolean {
        for (listener in listeners) {
            if (listener.onDown(e)) {
                return true
            }
        }
        return false
    }

    @Synchronized
    override fun onDoubleTap(e: MotionEvent): Boolean {
        for (listener in listeners) {
            if (listener.onDoubleTap(e)) {
                return true
            }
        }
        return false
    }

    @Synchronized
    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        for (listener in listeners) {
            if (listener.onDoubleTapEvent(e)) {
                return true
            }
        }
        return false
    }

    @Synchronized
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        for (listener in listeners) {
            if (listener.onSingleTapConfirmed(e)) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Synchronized
    override fun onContextClick(e: MotionEvent): Boolean {
        for (listener in listeners) {
            if (listener.onContextClick(e)) {
                return true
            }
        }
        return false
    }
}