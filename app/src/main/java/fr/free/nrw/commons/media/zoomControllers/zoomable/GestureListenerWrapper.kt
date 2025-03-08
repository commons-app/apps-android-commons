package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.view.GestureDetector
import android.view.MotionEvent

/** Wrapper for SimpleOnGestureListener as GestureDetector does not allow changing its listener. */
class GestureListenerWrapper : GestureDetector.SimpleOnGestureListener() {

    private var delegate: GestureDetector.SimpleOnGestureListener =
        GestureDetector.SimpleOnGestureListener()

    fun setListener(listener: GestureDetector.SimpleOnGestureListener) {
        delegate = listener
    }

    override fun onLongPress(e: MotionEvent) {
        delegate.onLongPress(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return delegate.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return delegate.onFling(e1, e2, velocityX, velocityY)
    }

    override fun onShowPress(e: MotionEvent) {
        delegate.onShowPress(e)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return delegate.onDown(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return delegate.onDoubleTap(e)
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return delegate.onDoubleTapEvent(e)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return delegate.onSingleTapConfirmed(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return delegate.onSingleTapUp(e)
    }
}
