package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewParent
import com.facebook.common.logging.FLog

/**
 * Handles touch gestures, tap detection, and event dispatch for a zoomable view.
 */
class ZoomableTouchHandler(
    context: Context,
    private val logTag: Class<*>,
    private val viewHashCodeProvider: () -> Int,
    private val zoomableControllerProvider: () -> ZoomableController?,
    private val parentProvider: () -> ViewParent?,
    private val superOnTouchEvent: (MotionEvent) -> Boolean,
) {
    private val tapListenerWrapper = GestureListenerWrapper()
    private var tapGestureDetector: GestureDetector? = GestureDetector(context, tapListenerWrapper)

    /**
     * Whether dial tone feedback or interception behavior is enabled.
     */
    var isDialToneEnabled: Boolean = false

    /**
     * Whether touch interception is allowed while the view is zoomed in.
     */
    var allowTouchInterceptionWhileZoomed: Boolean = true

    /**
     * Sets the custom tap listener wrapped by the internal gesture detector.
     *
     * @param tapListener the listener to receive tap events
     */
    fun setTapListener(tapListener: GestureDetector.SimpleOnGestureListener) {
        tapListenerWrapper.setListener(tapListener)
    }

    /**
     * Enables or disables long press detection on the tap gesture detector.
     *
     * @param enabled true to enable long press, false otherwise
     */
    fun setIsLongpressEnabled(enabled: Boolean) {
        tapGestureDetector?.setIsLongpressEnabled(enabled)
    }

    /**
     * Dispatches touch events across the tap gesture detector, zoomable controller,
     * and fallback view hierarchy.
     *
     * @param event the motion event being dispatched
     * @return true if the event was consumed, false otherwise
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val viewHashCode = viewHashCodeProvider()
        val zoomableController = zoomableControllerProvider()

        FLog.v(logTag, "onTouchEvent: %d, view %d, received", action, viewHashCode)

        val handled =
            when {
                !isDialToneEnabled && tapGestureDetector?.onTouchEvent(event) == true -> {
                    FLog.v(logTag, "onTouchEvent: %d, view %d, handled by tap gesture detector", action, viewHashCode)
                    true
                }
                !isDialToneEnabled && zoomableController?.onTouchEvent(event) == true -> {
                    FLog.v(logTag, "onTouchEvent: %d, view %d, handled by zoomable controller", action, viewHashCode)
                    if (!allowTouchInterceptionWhileZoomed && !zoomableController.isIdentity()) {
                        parentProvider()?.requestDisallowInterceptTouchEvent(true)
                    }
                    true
                }
                superOnTouchEvent(event) -> {
                    FLog.v(logTag, "onTouchEvent: %d, view %d, handled by the super", action, viewHashCode)
                    true
                }
                else -> {
                    val cancelEvent = MotionEvent.obtain(event).apply { this.action = MotionEvent.ACTION_CANCEL }
                    tapGestureDetector?.onTouchEvent(cancelEvent)
                    zoomableController?.onTouchEvent(cancelEvent)
                    cancelEvent.recycle()
                    false
                }
            }

        return handled
    }
}
