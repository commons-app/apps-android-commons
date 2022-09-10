package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
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
    private lateinit var motionEvent: MotionEvent

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
        gesListener.onDown(motionEvent)
    }

    /**
     * Test onFling
     */
    @Test
    fun onFling() {
        gesListener.onFling(motionEvent, motionEvent, 0f, 0f)
    }
}