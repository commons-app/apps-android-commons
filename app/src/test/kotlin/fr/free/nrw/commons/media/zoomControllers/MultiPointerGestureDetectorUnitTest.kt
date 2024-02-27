package fr.free.nrw.commons.media.zoomControllers

import android.view.MotionEvent
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.media.zoomControllers.gestures.MultiPointerGestureDetector
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import java.lang.reflect.Method

class MultiPointerGestureDetectorUnitTest {

    private lateinit var detector: MultiPointerGestureDetector

    @Mock
    private lateinit var listener: MultiPointerGestureDetector.Listener

    @Mock
    private lateinit var event: MotionEvent

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        detector = MultiPointerGestureDetector()
        detector = MultiPointerGestureDetector.newInstance()
        detector.setListener(listener)

        Whitebox.setInternalState(detector, "mGestureInProgress", false)
    }

    @Test
    @Throws(Exception::class)
    fun checkDetectorNotNull() {
        assertThat(detector, notNullValue())
    }

    @Test
    @Throws(Exception::class)
    fun testSetAvatarCaseNull() {
        val method: Method = MultiPointerGestureDetector::class.java.getDeclaredMethod(
            "shouldStartGesture"
        )
        method.isAccessible = true
        assertThat(method.invoke(detector), equalTo( true))
    }

    @Test
    @Throws(Exception::class)
    fun testStartGesture() {
        val method: Method = MultiPointerGestureDetector::class.java.getDeclaredMethod(
            "startGesture"
        )
        method.isAccessible = true
        method.invoke(detector)
        verify(listener).onGestureBegin(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testStopGesture() {
        Whitebox.setInternalState(detector, "mGestureInProgress", true)
        val method: Method = MultiPointerGestureDetector::class.java.getDeclaredMethod(
            "stopGesture"
        )
        method.isAccessible = true
        method.invoke(detector)
        verify(listener).onGestureEnd(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdatePointersOnTap() {
        whenever(event.pointerCount).thenReturn(3)
        whenever(event.actionMasked).thenReturn(MotionEvent.ACTION_UP)
        val method: Method = MultiPointerGestureDetector::class.java.getDeclaredMethod(
            "updatePointersOnTap", MotionEvent::class.java
        )
        method.isAccessible = true
        method.invoke(detector, event)
        verify(event, times(2)).actionIndex
    }

    @Test
    @Throws(Exception::class)
    fun testRestartGestureCaseReturn() {
        detector.restartGesture()
    }

    @Test
    @Throws(Exception::class)
    fun testRestartGesture() {
        Whitebox.setInternalState(detector, "mGestureInProgress", true)
        detector.restartGesture()
        verify(listener).onGestureBegin(detector)
        verify(listener).onGestureEnd(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testIsGestureInProgress() {
        assertThat(detector.isGestureInProgress, equalTo( false))
    }

    @Test
    @Throws(Exception::class)
    fun testGetNewPointerCount() {
        assertThat(detector.newPointerCount, equalTo( 0))
    }

    @Test
    @Throws(Exception::class)
    fun testGetPointerCount() {
        assertThat(detector.pointerCount, equalTo( 0))
    }

    @Test
    @Throws(Exception::class)
    fun testGetStartX() {
        assertThat(detector.startX[0], equalTo( 0.0f))
    }

    @Test
    @Throws(Exception::class)
    fun testGetStartY() {
        assertThat(detector.startY[0], equalTo( 0.0f))
    }

    @Test
    @Throws(Exception::class)
    fun testGetCurrentX() {
        assertThat(detector.currentX[0], equalTo( 0.0f))
    }

    @Test
    @Throws(Exception::class)
    fun testGetCurrentY() {
        assertThat(detector.currentY[0], equalTo( 0.0f))
    }

    @Test
    @Throws(Exception::class)
    fun testOnTouchEvent() {
        assertThat(detector.onTouchEvent(event), equalTo( true))
    }

    @Test
    @Throws(Exception::class)
    fun `testOnTouchEventCase ACTION_MOVE`() {
        Whitebox.setInternalState(detector, "mPointerCount", 1)
        whenever(event.actionMasked).thenReturn(MotionEvent.ACTION_MOVE)
        assertThat(detector.onTouchEvent(event), equalTo( true))
    }

    @Test
    @Throws(Exception::class)
    fun `testOnTouchEventCase ACTION_UP`() {
        whenever(event.pointerCount).thenReturn(3)
        Whitebox.setInternalState(detector, "mPointerCount", 1)
        whenever(event.actionMasked).thenReturn(MotionEvent.ACTION_UP)
        whenever(event.pointerCount).thenReturn(-2)
        assertThat(detector.onTouchEvent(event), equalTo( true))
    }

    @Test
    @Throws(Exception::class)
    fun `testOnTouchEventCase ACTION_CANCEL`() {
        whenever(event.actionMasked).thenReturn(MotionEvent.ACTION_CANCEL)
        assertThat(detector.onTouchEvent(event), equalTo( true))
    }

}