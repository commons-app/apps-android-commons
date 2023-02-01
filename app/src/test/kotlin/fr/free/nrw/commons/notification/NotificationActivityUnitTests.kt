package fr.free.nrw.commons.notification

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ShadowActionBar
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.notification.models.Notification
import fr.free.nrw.commons.notification.models.NotificationType
import fr.free.nrw.commons.utils.NetworkUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import org.wikipedia.AppAdapter
import java.lang.reflect.Field
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class, shadows = [ShadowActionBar::class])
class NotificationActivityUnitTests {

    @Mock
    private lateinit var activity: NotificationActivity

    @Mock
    private lateinit var notification: Notification

    @Mock
    private lateinit var menuItem: MenuItem

    private lateinit var networkUtils: NetworkUtils
    private lateinit var context: Context
    private lateinit var menuItemWithId: MenuItem
    private lateinit var menuItemWithoutId: MenuItem
    private lateinit var menu: Menu
    private var notificationWorkerFragment = NotificationWorkerFragment()
    private var notificationList =
        listOf(Notification(NotificationType.UNKNOWN, "", "", "", "", ""))

    @Before
    fun setUp() {

        MockitoAnnotations.initMocks(this)

        networkUtils = mock(NetworkUtils::class.java)

        AppAdapter.set(TestAppAdapter())

        val intent = Intent().putExtra("title", "read")

        activity =
            Robolectric.buildActivity(NotificationActivity::class.java, intent).create().get()

        context = ApplicationProvider.getApplicationContext()

        menuItemWithId = RoboMenuItem(R.id.archived)

        menuItemWithoutId = RoboMenuItem(null)

        menu = RoboMenu(context)

        val notificationMenuItem: Field =
            NotificationActivity::class.java.getDeclaredField("notificationMenuItem")
        notificationMenuItem.isAccessible = true
        notificationMenuItem.set(activity, menuItemWithId)

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
    fun checkRemoveNotificationIsReadTrue() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, true)
        activity.removeNotification(notification)
    }

    @Test
    @Throws(Exception::class)
    fun checkRemoveNotificationIsReadFalse() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, false)
        activity.removeNotification(notification)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateOptionsMenu() {
        activity.onCreateOptionsMenu(menu)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedDefault() {
        activity.onOptionsItemSelected(menuItemWithoutId)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedTitleCaseRead() {
        `when`(menuItem.itemId).thenReturn(R.id.archived)
        `when`(menuItem.title).thenReturn(context.getString(R.string.menu_option_read))
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedTitleCaseUnread() {
        `when`(menuItem.itemId).thenReturn(R.id.archived)
        `when`(menuItem.title).thenReturn(context.getString(R.string.menu_option_unread))
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testSetEmptyViewIsReadTrue() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, true)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setEmptyView"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetEmptyViewIsReadFalse() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, false)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setEmptyView"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetMenuItemTitleIsReadTrue() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, true)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setMenuItemTitle"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetMenuItemTitleIsReadFalse() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, false)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setMenuItemTitle"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetPageTitleIsReadTrue() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, true)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setPageTitle"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetPageTitleIsReadFalse() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, false)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setPageTitle"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetItemsListNull() {
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setItems", List::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
    }

    @Test
    @Throws(Exception::class)
    fun testSetItemsListNonNull() {
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "setItems", List::class.java
        )
        method.isAccessible = true
        method.invoke(activity, notificationList)
    }

    @Test
    @Throws(Exception::class)
    fun testHandleUrlNull() {
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "handleUrl", String::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
    }

    @Test
    @Throws(Exception::class)
    fun testHandleUrlNonNull() {
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "handleUrl", String::class.java
        )
        method.isAccessible = true
        method.invoke(activity, "string")
    }

    @Test
    @Throws(Exception::class)
    fun checkAddNotificationsNull() {
        val mNotificationWorkerFragment: Field =
            NotificationActivity::class.java.getDeclaredField("mNotificationWorkerFragment")
        mNotificationWorkerFragment.isAccessible = true
        mNotificationWorkerFragment.set(activity, null)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "addNotifications", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, true)
    }

    @Test
    @Throws(Exception::class)
    fun checkAddNotificationsNonNull() {
        val mNotificationWorkerFragment: Field =
            NotificationActivity::class.java.getDeclaredField("mNotificationWorkerFragment")
        mNotificationWorkerFragment.isAccessible = true
        mNotificationWorkerFragment.set(activity, notificationWorkerFragment)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "addNotifications", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, true)
    }

    @Test
    @Throws(Exception::class)
    fun testInitListViewIsReadFalse() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, false)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "initListView"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testInitListViewIsReadTrue() {
        val isRead: Field =
            NotificationActivity::class.java.getDeclaredField("isRead")
        isRead.isAccessible = true
        isRead.set(activity, true)
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "initListView"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshNull() {
        val method: Method = NotificationActivity::class.java.getDeclaredMethod(
            "refresh", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, true)
    }


}