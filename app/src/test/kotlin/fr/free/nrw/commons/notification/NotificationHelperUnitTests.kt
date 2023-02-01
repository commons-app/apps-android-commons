package fr.free.nrw.commons.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.notification.models.Notification
import fr.free.nrw.commons.notification.models.NotificationType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class NotificationHelperUnitTests {

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var context: Context

    @Mock
    private lateinit var intent: Intent

    @Mock
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        notificationHelper = NotificationHelper(context)

        val fieldNotificationManager: Field =
            NotificationHelper::class.java.getDeclaredField("notificationManager")
        fieldNotificationManager.isAccessible = true
        fieldNotificationManager.set(notificationHelper, notificationManager)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(notificationHelper)
    }

    @Test
    @Throws(Exception::class)
    fun testShowNotification() {
        notificationHelper.showNotification(context, "", "", 0, intent)
    }

    @Test
    @Throws(Exception::class)
    fun testNotificationConstructorTypeUnknown() {
        Notification(
            notificationType = NotificationType.handledValueOf(""),
            notificationText = "",
            date = "",
            link = "",
            iconUrl = "",
            notificationId = ""
        )
    }

    @Test
    @Throws(Exception::class)
    fun testNotificationConstructorTypeMention() {
        Notification(
            notificationType = NotificationType.handledValueOf("mention"),
            notificationText = "",
            date = "",
            link = "",
            iconUrl = "",
            notificationId = ""
        )
    }

}