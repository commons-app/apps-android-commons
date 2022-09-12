package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wikipedia.AppAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
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

        context = RuntimeEnvironment.application.applicationContext
        onSwipeTouchListener = OnSwipeTouchListener(context)
        gesListener = OnSwipeTouchListener(context).GestureListener()

        Whitebox.setInternalState(onSwipeTouchListener, "gestureDetector", gestureDetector)

    }

    /**
     * Test onTouch
     */
    @Test
    fun onTouch() {
        val func = onSwipeTouchListener.javaClass.getDeclaredMethod("onTouch", View::class.java, MotionEvent::class.java)
        func.isAccessible = true
        func.invoke(onSwipeTouchListener, view, motionEvent1)
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