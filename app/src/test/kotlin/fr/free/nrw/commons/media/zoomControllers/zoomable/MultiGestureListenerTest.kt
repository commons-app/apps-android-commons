package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.view.GestureDetector
import android.view.MotionEvent
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiGestureListenerTest {
    @Test
    fun onScrollStopsAfterAListenerConsumesTheEvent() {
        val listener = MultiGestureListener()
        val first = mock<GestureDetector.SimpleOnGestureListener>()
        val second = mock<GestureDetector.SimpleOnGestureListener>()
        val start = mock<MotionEvent>()
        val end = mock<MotionEvent>()
        whenever(first.onScroll(start, end, 1f, 2f)).thenReturn(true)
        listener.childListeners.add(first)
        listener.childListeners.add(second)

        assertTrue(listener.onScroll(start, end, 1f, 2f))

        verify(second, never()).onScroll(start, end, 1f, 2f)
    }

    @Test
    fun onLongPressNotifiesEveryListener() {
        val listener = MultiGestureListener()
        val first = mock<GestureDetector.SimpleOnGestureListener>()
        val second = mock<GestureDetector.SimpleOnGestureListener>()
        val event = mock<MotionEvent>()
        listener.childListeners.add(first)
        listener.childListeners.add(second)

        listener.onLongPress(event)

        verify(first).onLongPress(event)
        verify(second).onLongPress(event)
    }

    @Test
    fun removeDropsTheListenerFromLaterEvents() {
        val listener = MultiGestureListener()
        val first = mock<GestureDetector.SimpleOnGestureListener>()
        val event = mock<MotionEvent>()
        listener.childListeners.add(first)
        listener.childListeners.remove(first)

        assertFalse(listener.onSingleTapUp(event))

        verify(first, never()).onSingleTapUp(event)
    }
}
