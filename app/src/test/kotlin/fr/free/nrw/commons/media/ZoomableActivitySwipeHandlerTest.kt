package fr.free.nrw.commons.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.customselector.model.Image
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for [ZoomableActivitySwipeHandler], covering the swipe gestures that
 * navigate between images and select or skip them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ZoomableActivitySwipeHandlerTest {
    private lateinit var activity: ZoomableActivity
    private lateinit var swipeHandler: ZoomableActivitySwipeHandler
    private lateinit var image: Image
    private lateinit var nextImage: Image

    @Mock
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        OkHttpConnectionFactory.CLIENT = createTestClient()
        val context: Context = ApplicationProvider.getApplicationContext()
        SoLoader.setInTestMode()
        Fresco.initialize(context)
        val intent = Intent().setData(uri)
        activity = Robolectric.buildActivity(ZoomableActivity::class.java, intent).create().get()

        image = Image(1, "image", uri, "abc/abc", 1, "bucket1")
        nextImage = Image(2, "image2", uri, "abc/abc2", 1, "bucket1")

        Whitebox.setInternalState(activity, "images", arrayListOf(image))
        Whitebox.setInternalState(activity, "selectedImages", arrayListOf(image))
        swipeHandler = Whitebox.getInternalState(activity, "swipeHandler")
    }

    /**
     * Test that the handler is created by the activity.
     */
    @Test
    fun checkHandlerNotNull() {
        Assert.assertNotNull(swipeHandler)
    }

    /**
     * Test onSwipe.
     */
    @Test
    fun testOnSwipe() {
        swipeHandler.onSwipe()
    }

    /**
     * Test onLeftSwiped.
     */
    @Test
    fun testOnLeftSwiped() {
        swipeHandler.onLeftSwiped(true)

        Whitebox.setInternalState(activity, "images", arrayListOf(image, nextImage))
        Whitebox.setInternalState(activity, "position", 0)
        swipeHandler.onLeftSwiped(true)
        Assert.assertEquals(1, activity.position)

        swipeHandler.onLeftSwiped(false)
        Assert.assertEquals(1, activity.position)
    }

    /**
     * Test onRightSwiped.
     */
    @Test
    fun testOnRightSwiped() {
        swipeHandler.onRightSwiped(true)

        Whitebox.setInternalState(activity, "images", arrayListOf(image, nextImage))
        Whitebox.setInternalState(activity, "position", 1)
        swipeHandler.onRightSwiped(true)
        Assert.assertEquals(0, activity.position)

        swipeHandler.onRightSwiped(false)
        Assert.assertEquals(0, activity.position)
    }

    /**
     * Test onUpSwiped.
     */
    @Test
    fun testOnUpSwiped() {
        swipeHandler.onUpSwiped()
    }

    /**
     * Test onDownSwiped.
     */
    @Test
    fun testOnDownSwiped() {
        swipeHandler.onDownSwiped()
    }
}
