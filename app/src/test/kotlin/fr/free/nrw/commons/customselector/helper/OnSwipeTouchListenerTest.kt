package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.TestUtility.setFinalStatic
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wikipedia.AppAdapter
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@PrepareForTest(OnSwipeTouchListener::class)
internal class OnSwipeTouchListenerTest {

    private lateinit var context: Context
    private lateinit var onSwipeTouchListener: OnSwipeTouchListener
    private lateinit var gesListener: OnSwipeTouchListener.GestureListener

    @Mock
    private lateinit var gestureDetector: GestureDetector

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var motionEvent1: MotionEvent

    @Mock
    private lateinit var motionEvent2: MotionEvent

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        AppAdapter.set(TestAppAdapter())
//        motionEvent1 = MotionEvent.obtain(200, 300, MotionEvent.ACTION_MOVE, 15.0f, 10.0f, 0);

        context = ApplicationProvider.getApplicationContext()
        onSwipeTouchListener = OnSwipeTouchListener(context)
        gesListener = OnSwipeTouchListener(context).GestureListener()
        setFinalStatic(
                OnSwipeTouchListener::class.java.getDeclaredField("gestureDetector"),
                gestureDetector)
    }

    /**
     * Test onTouch
     */
    @Test
    fun onTouch() {
        val motionEvent = MotionEvent.obtain(200, 300, MotionEvent.ACTION_MOVE, 15.0f, 10.0f, 0);
        val func = onSwipeTouchListener.javaClass.getDeclaredMethod("onTouch", View::class.java, MotionEvent::class.java)
        func.isAccessible = true
        func.invoke(onSwipeTouchListener, view, motionEvent)
    }


    /**
     * Test onSwipeRight
     */
    @Test
    fun onSwipeRight() {
        onSwipeTouchListener.onSwipeRight()
    }

    /**
     * Test onSwipeLeft
     */
    @Test
    fun onSwipeLeft() {
        onSwipeTouchListener.onSwipeLeft()
    }

    /**
     * Test onSwipeUp
     */
    @Test
    fun onSwipeUp() {
        onSwipeTouchListener.onSwipeUp()
    }

    /**
     * Test onSwipeDown
     */
    @Test
    fun onSwipeDown() {
        onSwipeTouchListener.onSwipeDown()
    }

    /**
     * Test onDown
     */
    @Test
    fun onDown() {
        gesListener.onDown(motionEvent1)
    }

    /**
     * Test onFling for onSwipeRight
     */
    @Test
    fun `Test onFling for onSwipeRight`() {
        whenever(motionEvent1.x).thenReturn(1f)
        whenever(motionEvent2.x).thenReturn(110f)
        gesListener.onFling(motionEvent1, motionEvent2, 2000f, 0f)
    }

    /**
     * Test onFling for onSwipeLeft
     */
    @Test
    fun `Test onFling for onSwipeLeft`() {
        whenever(motionEvent1.x).thenReturn(110f)
        whenever(motionEvent2.x).thenReturn(1f)
        gesListener.onFling(motionEvent1, motionEvent2, 2000f, 0f)
    }

    /**
     * Test onFling for onSwipeDown
     */
    @Test
    fun `Test onFling for onSwipeDown`() {
        whenever(motionEvent1.y).thenReturn(1f)
        whenever(motionEvent2.y).thenReturn(110f)
        gesListener.onFling(motionEvent1, motionEvent2, 0f, 2000f)
    }

    /**
     * Test onFling for onSwipeUp
     */
    @Test
    fun `Test onFling for onSwipeUp`() {
        whenever(motionEvent1.y).thenReturn(110f)
        whenever(motionEvent2.y).thenReturn(1f)
        gesListener.onFling(motionEvent1, motionEvent2, 0f, 2000f)
    }
}