package fr.free.nrw.commons.profile

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ProfileActivityTest {

    private lateinit var activity: ProfileActivity
    private lateinit var profileActivity: ProfileActivity
    private lateinit var mockContext: Context
    private lateinit var menuItem: MenuItem
    private lateinit var menu: Menu

    @Mock
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        mockContext = PowerMockito.mock(Context::class.java)
        profileActivity = PowerMockito.mock(ProfileActivity::class.java)
        menuItem = RoboMenuItem(null)
        menu = RoboMenu(mockContext)
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
        activity.onCreateOptionsMenu(menu)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testStartYourself() {
        ProfileActivity.startYourself(mockContext)
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