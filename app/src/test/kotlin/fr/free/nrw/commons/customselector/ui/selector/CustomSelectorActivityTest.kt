package fr.free.nrw.commons.customselector.ui.selector

import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.Looper.getMainLooper
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
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

    /**
     * Set up the tests.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        AppAdapter.set(TestAppAdapter())

        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java)
            .get()
        val onCreate = activity.javaClass.getDeclaredMethod("onCreate", Bundle::class.java)
        onCreate.isAccessible = true
        onCreate.invoke(activity, null)
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