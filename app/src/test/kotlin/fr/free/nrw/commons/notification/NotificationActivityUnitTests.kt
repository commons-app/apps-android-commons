package fr.free.nrw.commons.notification

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import fr.free.nrw.commons.TestAppAdapter
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
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import org.wikipedia.AppAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class NotificationActivityUnitTests {

    @Mock
    private lateinit var activity: NotificationActivity

    @Mock
    private lateinit var notification: Notification

    private lateinit var context: Context

    private lateinit var menuItem: MenuItem

    private lateinit var menu: Menu

    @Before
    fun setUp() {

        MockitoAnnotations.initMocks(this)

        AppAdapter.set(TestAppAdapter())

        val intent = Intent().putExtra("title", "read")

        activity =
            Robolectric.buildActivity(NotificationActivity::class.java, intent).create().get()

        context = RuntimeEnvironment.application.applicationContext

        menuItem = RoboMenuItem(null)

        menu = RoboMenu(context)

    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun checkOnSupportNavigateUp() {
        activity.onSupportNavigateUp()
    }

    @Test
    @Throws(Exception::class)
    fun checkRemoveNotification() {
        activity.removeNotification(notification)
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

}