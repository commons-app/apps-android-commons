package fr.free.nrw.commons.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
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
import org.wikipedia.AppAdapter
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ZoomableActivityUnitTests {

    private lateinit var context: Context
    private lateinit var activity: ZoomableActivity
    private lateinit var viewModelField: Field
    private lateinit var image: Image

    @Mock
    private lateinit var uri: Uri

    @Mock
    private lateinit var images: ArrayList<Image>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        AppAdapter.set(TestAppAdapter())
        context = ApplicationProvider.getApplicationContext()
        SoLoader.setInTestMode()
        Fresco.initialize(context)
        val intent = Intent().setData(uri)
        activity = Robolectric.buildActivity(ZoomableActivity::class.java, intent).create().get()

        image = Image(1, "image", uri, "abc/abc", 1, "bucket1")

        Whitebox.setInternalState(activity, "images", arrayListOf(image))
        Whitebox.setInternalState(activity, "selectedImages", arrayListOf(image))

        viewModelField = activity.javaClass.getDeclaredField("viewModel")
        viewModelField.isAccessible = true
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    /**
     * Test handleResult.
     */
    @Test
    fun testHandleResult(){
        val func = activity.javaClass.getDeclaredMethod("handleResult", Result::class.java)
        func.isAccessible = true
        func.invoke(activity, Result(CallbackStatus.SUCCESS, arrayListOf()))
        func.invoke(activity, Result(CallbackStatus.SUCCESS, arrayListOf(image,image)))
    }

    /**
     * Test onLeftSwiped.
     */
    @Test
    fun testOnLeftSwiped(){
        val func = activity.javaClass.getDeclaredMethod("onLeftSwiped", Boolean::class.java)
        func.isAccessible = true
        func.invoke(activity, true)

        Whitebox.setInternalState(activity, "images", arrayListOf(image, image))
        Whitebox.setInternalState(activity, "position", 0)
        func.invoke(activity, true)

        func.invoke(activity, false)
    }

    /**
     * Test onRightSwiped.
     */
    @Test
    fun testOnRightSwiped(){
        val func = activity.javaClass.getDeclaredMethod("onRightSwiped", Boolean::class.java)
        func.isAccessible = true
        func.invoke(activity, true)

        Whitebox.setInternalState(activity, "images", arrayListOf(image, image))
        Whitebox.setInternalState(activity, "position", 1)
        func.invoke(activity, true)

        func.invoke(activity, false)
    }

    /**
     * Test onUpSwiped.
     */
    @Test
    fun testOnUpSwiped(){
        val func = activity.javaClass.getDeclaredMethod("onUpSwiped")
        func.isAccessible = true
        func.invoke(activity)
    }

    /**
     * Test onDownSwiped.
     */
    @Test
    fun testOnDownSwiped(){
        val func = activity.javaClass.getDeclaredMethod("onDownSwiped")
        func.isAccessible = true
        func.invoke(activity)
    }

    /**
     * Test onBackPressed.
     */
    @Test
    fun testOnBackPressed(){
        val func = activity.javaClass.getDeclaredMethod("onBackPressed")
        func.isAccessible = true
        func.invoke(activity)
    }


    /**
     * Test onDestroy.
     */
    @Test
    fun testOnDestroy(){
        val func = activity.javaClass.getDeclaredMethod("onDestroy")
        func.isAccessible = true
        func.invoke(activity)
    }
}