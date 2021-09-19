package fr.free.nrw.commons.profile

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ProfileActivityTest {

    @Mock
    private lateinit var activity: ProfileActivity

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        mockContext = RuntimeEnvironment.application.applicationContext
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        activity.onDestroy()
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateOptionsMenu() {
        val menu: Menu = RoboMenu(mockContext)
        activity.onCreateOptionsMenu(menu)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        val menuItem: MenuItem = RoboMenuItem(R.menu.menu_about)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsShareItemSelected() {
        val menuItemShare: MenuItem = RoboMenuItem(R.id.share_app_icon)
        activity.onOptionsItemSelected(menuItemShare)
    }

    @Test
    @Throws(Exception::class)
    fun testStartYourself() {
        ProfileActivity.startYourself(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testShowAlert() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        activity.showAlert(bitmap)
    }

    @Test
    @Throws(Exception::class)
    fun testShareScreen() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ProfileActivity::class.java.getDeclaredMethod(
            "shareScreen", Bitmap::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bitmap)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSupportNavigateUp() {
        activity.onSupportNavigateUp()
    }
}