package fr.free.nrw.commons.media.zoomControllers

import android.view.MotionEvent
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import fr.free.nrw.commons.media.zoomControllers.gestures.MultiPointerGestureDetector
import fr.free.nrw.commons.media.zoomControllers.gestures.TransformGestureDetector
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import java.lang.reflect.Method

class TransformGestureDetectorUnitTest {
    private lateinit var detector: TransformGestureDetector

    @Mock
    private lateinit var listener: TransformGestureDetector.Listener

    @Mock
    private lateinit var mDetector: MultiPointerGestureDetector

    @Mock
    private lateinit var event: MotionEvent

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        detector = TransformGestureDetector(MultiPointerGestureDetector())
        detector = TransformGestureDetector.newInstance()
        detector.setListener(listener)
    }

    @Test
    @Throws(Exception::class)
    fun checkDetectorNotNull() {
        Assert.assertNotNull(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testReset() {
        Whitebox.setInternalState(detector, "mDetector", mDetector)
        detector.reset()
        verify(mDetector).reset()
    }

    @Test
    @Throws(Exception::class)
    fun testOnTouchEvent() {
        whenever(mDetector.onTouchEvent(event)).thenReturn(true)
        Assert.assertEquals(detector.onTouchEvent(event), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnGestureBegin() {
        detector.onGestureBegin(mDetector)
        verify(listener).onGestureBegin(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testOnGestureUpdate() {
        detector.onGestureUpdate(mDetector)
        verify(listener).onGestureUpdate(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testOnGestureEnd() {
        detector.onGestureEnd(mDetector)
        verify(listener).onGestureEnd(detector)
    }

    @Test
    @Throws(Exception::class)
    fun testRestartGesture() {
        detector.restartGesture()
    }

    @Test
    @Throws(Exception::class)
    fun testIsGestureInProgress() {
        Assert.assertEquals(detector.isGestureInProgress(), false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetNewPointerCount() {
        Assert.assertEquals(detector.getNewPointerCount(), 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPointerCount() {
        Assert.assertEquals(detector.getPointerCount(), 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPivotX() {
        Assert.assertEquals(detector.getPivotX(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPivotY() {
        Assert.assertEquals(detector.getPivotY(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTranslationX() {
        Assert.assertEquals(detector.getTranslationX(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTranslationY() {
        Assert.assertEquals(detector.getTranslationY(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetScaleCaseLessThan2() {
        Whitebox.setInternalState(detector, "mDetector", mDetector)
        whenever(mDetector.getPointerCount()).thenReturn(1)
        Assert.assertEquals(detector.getScale(), 1f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetScaleCaseGreaterThan2() {
        val array = FloatArray(2)
        array[0] = 0.0f
        array[1] = 1.0f
        Whitebox.setInternalState(detector, "mDetector", mDetector)
        whenever(mDetector.getPointerCount()).thenReturn(2)
        whenever(mDetector.getStartX()).thenReturn(array)
        whenever(mDetector.getStartY()).thenReturn(array)
        whenever(mDetector.getCurrentX()).thenReturn(array)
        whenever(mDetector.getCurrentY()).thenReturn(array)
        Assert.assertEquals(detector.getScale(), 1f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetRotationCaseLessThan2() {
        Whitebox.setInternalState(detector, "mDetector", mDetector)
        whenever(mDetector.getPointerCount()).thenReturn(1)
        Assert.assertEquals(detector.getRotation(), 0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetRotationCaseGreaterThan2() {
        val array = FloatArray(2)
        array[0] = 0.0f
        array[1] = 1.0f
        Whitebox.setInternalState(detector, "mDetector", mDetector)
        whenever(mDetector.getPointerCount()).thenReturn(2)
        whenever(mDetector.getStartX()).thenReturn(array)
        whenever(mDetector.getStartY()).thenReturn(array)
        whenever(mDetector.getCurrentX()).thenReturn(array)
        whenever(mDetector.getCurrentY()).thenReturn(array)
        Assert.assertEquals(detector.getRotation(), 0f)
    }

    @Test
    @Throws(Exception::class)
    fun testCalcAverage() {
        val array = FloatArray(2)
        array[0] = 0.0f
        array[1] = 1.0f
        val method: Method =
            TransformGestureDetector::class.java.getDeclaredMethod(
                "calcAverage",
                FloatArray::class.java,
                Int::class.java,
            )
        method.isAccessible = true
        Assert.assertEquals(method.invoke(detector, array, 2), 0.5f)
    }
}
