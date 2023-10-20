package fr.free.nrw.commons.customselector.ui.selector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

/**
 * Custom Selector Activity Test
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CustomSelectorActivityTest {

    private lateinit var activity: CustomSelectorActivity

    private lateinit var imageFragment: ImageFragment

    private lateinit var images : java.util.ArrayList<Image>

    private var uri: Uri = Mockito.mock(Uri::class.java)

    private lateinit var image: Image

    /**
     * Set up the tests.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        AppAdapter.set(TestAppAdapter())

        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java)
            .get()
        val onCreate = activity.javaClass.getDeclaredMethod("onCreate", Bundle::class.java)
        onCreate.isAccessible = true
        onCreate.invoke(activity, null)
        imageFragment = ImageFragment.newInstance(1,0)
        image = Image(1, "image", uri, "abc/abc", 1, "bucket1")
        images = ArrayList()

        Whitebox.setInternalState(activity, "imageFragment", imageFragment)
        Whitebox.setInternalState(imageFragment, "imageAdapter", Mockito.mock(ImageAdapter::class.java))
    }

    /**
     * Test activity not null.
     */
    @Test
    @Throws(Exception::class)
    fun testActivityNotNull() {
        assertNotNull(activity)
    }

    /**
     * Test changeTitle function.
     */
    @Test
    @Throws(Exception::class)
    fun testChangeTitle() {
        val func = activity.javaClass.getDeclaredMethod("changeTitle", String::class.java)
        func.isAccessible = true
        func.invoke(activity, "test")
    }

    /**
     * Test onFolderClick function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnFolderClick() {
        activity.onFolderClick(1, "test", 0);
    }

    /**
     * Test onActivityResult function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnActivityResult() {
        val func = activity.javaClass.getDeclaredMethod(
            "onActivityResult",
            Int::class.java,
            Int::class.java,
            Intent::class.java
        )
        func.isAccessible = true
        func.invoke(activity, 512, -1, Mockito.mock(Intent::class.java))
    }

    /**
     * Test showWelcomeDialog function.
     */
    @Test
    @Throws(Exception::class)
    fun testShowWelcomeDialog() {
        val func = activity.javaClass.getDeclaredMethod(
            "showWelcomeDialog"
        )
        func.isAccessible = true
        func.invoke(activity)
    }


    /**
     * Test onLongPress function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnLongPress() {
        val func = activity.javaClass.getDeclaredMethod(
            "onLongPress",
            Int::class.java,
            ArrayList::class.java,
            ArrayList::class.java
        )
        images.add(image)

        func.isAccessible = true
        func.invoke(activity, 0, images, images)
    }

    /**
     * Test selectedImagesChanged function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnSelectedImagesChanged() {
        activity.onSelectedImagesChanged(ArrayList(), 0)
    }

    /**
     * Test onDone function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnDone() {
        activity.onDone()
        activity.onSelectedImagesChanged(
            ArrayList(arrayListOf(Image(1, "test", Uri.parse("test"), "test", 1))),
            1
        )
        activity.onDone()
    }

    /**
     * Test onClickNotForUpload function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnClickNotForUpload() {
        val method: Method = CustomSelectorActivity::class.java.getDeclaredMethod(
            "onClickNotForUpload"
        )
        method.isAccessible = true
        method.invoke(activity)
        activity.onSelectedImagesChanged(
            ArrayList(arrayListOf(Image(1, "test", Uri.parse("test"), "test", 1))),
            0
        )
        method.invoke(activity)
    }

    /**
     * Test setOnDataListener Function.
     */
    @Test
    @Throws(Exception::class)
    fun testSetOnDataListener() {
        activity.setOnDataListener(imageFragment)
    }

    /**
     * Test onBackPressed Function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

    /**
     * Test onDestroy Function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        val method: Method = CustomSelectorActivity::class.java.getDeclaredMethod(
            "onDestroy"
        )
        method.isAccessible = true
        method.invoke(activity)
    }
}