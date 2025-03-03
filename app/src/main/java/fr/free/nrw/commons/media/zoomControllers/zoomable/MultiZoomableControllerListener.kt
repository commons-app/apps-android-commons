package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.graphics.Matrix
import java.util.ArrayList

/**
 * MultiZoomableControllerListener that allows multiple listeners to be added and notified about
 * transform events.
 *
 * NOTE: The order of the listeners is important. Listeners can consume transform events.
 */
class MultiZoomableControllerListener : ZoomableController.Listener {

    private val listeners: MutableList<ZoomableController.Listener> = mutableListOf()

    @Synchronized
    override fun onTransformBegin(transform: Matrix) {
        for (listener in listeners) {
            listener.onTransformBegin(transform)
        }
    }

    @Synchronized
    override fun onTransformChanged(transform: Matrix) {
        for (listener in listeners) {
            listener.onTransformChanged(transform)
        }
    }

    @Synchronized
    override fun onTransformEnd(transform: Matrix) {
        for (listener in listeners) {
            listener.onTransformEnd(transform)
        }
    }

    @Synchronized
    fun addListener(listener: ZoomableController.Listener) {
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: ZoomableController.Listener) {
        listeners.remove(listener)
    }
}
