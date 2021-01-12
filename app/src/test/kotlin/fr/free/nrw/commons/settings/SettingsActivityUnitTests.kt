package fr.free.nrw.commons.settings

import android.content.Context
import android.view.MenuItem
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class SettingsActivityUnitTests {

    private lateinit var activity: SettingsActivity

    private lateinit var context: Context

    private lateinit var menuItem: MenuItem

    @Before
    fun setUp() {

        context = RuntimeEnvironment.application.applicationContext

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
    fun testOnOptionsItemSelected() {
        activity.onOptionsItemSelected(menuItem)
    }

}
