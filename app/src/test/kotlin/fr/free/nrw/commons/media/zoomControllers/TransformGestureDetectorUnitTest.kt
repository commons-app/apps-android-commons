package fr.free.nrw.commons.media.zoomControllers

import android.view.MotionEvent
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.media.zoomControllers.gestures.GestureTransform
import fr.free.nrw.commons.media.zoomControllers.gestures.MultiPointerGestureDetector
import fr.free.nrw.commons.media.zoomControllers.gestures.PointerSlots
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
        val pointers = Whitebox.getInternalState<MultiPointerGestureDetector>(detector, "mDetector")
        Assert.assertEquals(pointers.isGestureInProgress(), false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetNewPointerCount() {
        val pointers = Whitebox.getInternalState<MultiPointerGestureDetector>(detector, "mDetector")
        Assert.assertEquals(pointers.pointerSlots.newPointerCount, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPointerCount() {
        val pointers = Whitebox.getInternalState<MultiPointerGestureDetector>(detector, "mDetector")
        Assert.assertEquals(pointers.pointerSlots.pointerCount, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPivotX() {
        Assert.assertEquals(detector.transform.getPivotX(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPivotY() {
        Assert.assertEquals(detector.transform.getPivotY(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTranslationX() {
        Assert.assertEquals(detector.transform.getTranslationX(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTranslationY() {
        Assert.assertEquals(detector.transform.getTranslationY(), 0.0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetScaleCaseLessThan2() {
        val transformDetector = transformWithPointerCount(1)
        Assert.assertEquals(transformDetector.transform.getScale(), 1f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetScaleCaseGreaterThan2() {
        val array = floatArrayOf(0.0f, 1.0f)
        val transformDetector = transformWithCoordinates(array, array, array, array)
        Assert.assertEquals(transformDetector.transform.getScale(), 1f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetScaleUsesDistanceRatio() {
        val transformDetector =
            transformWithCoordinates(
                startX = floatArrayOf(0.0f, 1.0f),
                startY = floatArrayOf(0.0f, 0.0f),
                currentX = floatArrayOf(0.0f, 2.0f),
                currentY = floatArrayOf(0.0f, 0.0f),
            )
        Assert.assertEquals(transformDetector.transform.getScale(), 2f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetRotationCaseLessThan2() {
        val transformDetector = transformWithPointerCount(1)
        Assert.assertEquals(transformDetector.transform.getRotation(), 0f)
    }

    @Test
    @Throws(Exception::class)
    fun testGetRotationCaseGreaterThan2() {
        val array = floatArrayOf(0.0f, 1.0f)
        val transformDetector = transformWithCoordinates(array, array, array, array)
        Assert.assertEquals(transformDetector.transform.getRotation(), 0f)
    }

    @Test
    @Throws(Exception::class)
    fun testCalcAverage() {
        val array = FloatArray(2)
        array[0] = 0.0f
        array[1] = 1.0f
        val method: Method =
            GestureTransform::class.java.getDeclaredMethod(
                "calcAverage",
                FloatArray::class.java,
                Int::class.java,
            )
        method.isAccessible = true
        val transform = GestureTransform(PointerSlots())
        Assert.assertEquals(method.invoke(transform, array, 2), 0.5f)
    }

    private fun transformWithPointerCount(count: Int): TransformGestureDetector =
        transformWithCoordinates(
            startX = FloatArray(2),
            startY = FloatArray(2),
            currentX = FloatArray(2),
            currentY = FloatArray(2),
            count = count,
        )

    private fun transformWithCoordinates(
        startX: FloatArray,
        startY: FloatArray,
        currentX: FloatArray,
        currentY: FloatArray,
        count: Int = 2,
    ): TransformGestureDetector {
        val pointers = MultiPointerGestureDetector.newInstance()
        val slots = pointers.pointerSlots
        Whitebox.setInternalState(slots, "pointerCount", count)
        Whitebox.setInternalState(slots, "mStartX", startX)
        Whitebox.setInternalState(slots, "mStartY", startY)
        Whitebox.setInternalState(slots, "mCurrentX", currentX)
        Whitebox.setInternalState(slots, "mCurrentY", currentY)
        return TransformGestureDetector(pointers)
    }
}
