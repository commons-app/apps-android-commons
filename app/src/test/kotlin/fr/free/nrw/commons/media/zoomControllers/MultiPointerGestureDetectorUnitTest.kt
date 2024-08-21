package fr.free.nrw.commons.media.zoomControllers

import android.view.MotionEvent
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.media.zoomControllers.gestures.MultiPointerGestureDetector
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
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
        Assert.assertNotNull(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testSetAvatarCaseNull() {
        val method: Method = MultiPointerGestureDetector::class.java.getDeclaredMethod(
            "shouldStartGesture"
        )
        method.isAccessible = true
        Assert.assertEquals(method.invoke(detector), true)
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
        Assert.assertEquals(detector.isGestureInProgress, false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetNewPointerCount() {
        Assert.assertEquals(detector.newPointerCount, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPointerCount() {
        Assert.assertEquals(detector.pointerCount, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetStartX() {
        Assert.assertEquals(detector.startX[0], 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetStartY() {
        Assert.assertEquals(detector.startY[0], 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCurrentX() {
        Assert.assertEquals(detector.currentX[0], 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCurrentY() {
        Assert.assertEquals(detector.currentY[0], 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testOnTouchEvent() {
        Assert.assertEquals(detector.onTouchEvent(event), true)
    }

    @Test
    @Throws(Exception::class)
    fun `testOnTouchEventCase ACTION_MOVE`() {
        Whitebox.setInternalState(detector, "mPointerCount", 1)
        whenever(event.actionMasked).thenReturn(MotionEvent.ACTION_MOVE)
        Assert.assertEquals(detector.onTouchEvent(event), true)
    }

    @Test
    @Throws(Exception::class)
    fun `testOnTouchEventCase ACTION_UP`() {
        whenever(event.pointerCount).thenReturn(3)
        Whitebox.setInternalState(detector, "mPointerCount", 1)
        whenever(event.actionMasked).thenReturn(MotionEvent.ACTION_UP)
        whenever(event.pointerCount).thenReturn(-2)
        Assert.assertEquals(detector.onTouchEvent(event), true)
    }

    @Test
    @Throws(Exception::class)
    fun `testOnTouchEventCase ACTION_CANCEL`() {
        whenever(event.actionMasked).thenReturn(MotionEvent.ACTION_CANCEL)
        Assert.assertEquals(detector.onTouchEvent(event), true)
    }

}