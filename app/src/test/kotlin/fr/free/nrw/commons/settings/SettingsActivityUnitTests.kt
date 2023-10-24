package fr.free.nrw.commons.settings

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.MenuItem
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboMenuItem
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SettingsActivityUnitTests {

    private lateinit var activity: SettingsActivity
    private lateinit var context: Context
    private lateinit var menuItem: MenuItem

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(SettingsActivity::class.java).create().get()
        menuItem = RoboMenuItem(null)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSupportNavigateUp() {
        activity.onSupportNavigateUp()
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseDefault() {
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseHome() {
        menuItem = RoboMenuItem(android.R.id.home)
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testSetTotalUploadCount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = SettingsActivity::class.java.getDeclaredMethod(
            "onPostCreate",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, savedInstanceState)
    }

}
