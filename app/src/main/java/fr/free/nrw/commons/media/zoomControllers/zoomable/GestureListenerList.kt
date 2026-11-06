package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.view.GestureDetector
import java.util.Collections.synchronizedList

/**
 * Child listeners notified by [MultiGestureListener], in the order they were added.
 *
 * A listener earlier in the list can consume an event so later listeners are not notified.
 */
class GestureListenerList {
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
    fun add(listener: GestureDetector.SimpleOnGestureListener) {
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
    fun remove(listener: GestureDetector.SimpleOnGestureListener) {
        listeners.remove(listener)
    }

    /**
     * Notifies listeners in order and stops at the first one that consumes the event.
     *
     * @return whether a listener consumed the event
     */
    @Synchronized
    fun firstConsuming(consume: (GestureDetector.SimpleOnGestureListener) -> Boolean): Boolean {
        for (listener in listeners) {
            if (consume(listener)) {
                return true
            }
        }
        return false
    }

    /** Notifies every listener. These callbacks cannot consume the event. */
    @Synchronized
    fun notifyEach(notify: (GestureDetector.SimpleOnGestureListener) -> Unit) {
        for (listener in listeners) {
            notify(listener)
        }
    }
}
