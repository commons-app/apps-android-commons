package fr.free.nrw.commons.upload.worker

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that upload progress notifications are silent while upload results still alert, see issue
 * #2528.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestCommonsApplication::class)
class UploadNotificationChannelsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun uploadProgressChannelIsSilent() {
        CommonsApplication.createNotificationChannel(context)

        val manager = context.getSystemService(NotificationManager::class.java)
        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            manager.getNotificationChannel(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS).importance,
        )
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            manager.getNotificationChannel(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL).importance,
        )
    }

    @Test
    fun progressNotificationsArePostedSilently() {
        val notification = builder().useUploadProgressChannel().build()

        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
        assertTrue(notification.alertsOnlyOnce())
    }

    @Test
    fun resultNotificationsAlertOnTheDefaultChannel() {
        val notification = builder().useUploadProgressChannel().useUploadResultChannel().build()

        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL, notification.channelId)
        assertFalse(notification.alertsOnlyOnce())
    }

    @Test
    fun nextUploadIsSilentAgainAfterAResult() {
        val notification =
            builder()
                .useUploadResultChannel()
                .useUploadProgressChannel()
                .build()

        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
        assertTrue(notification.alertsOnlyOnce())
    }

    private fun builder() =
        NotificationCompat
            .Builder(context, CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)
            .setSmallIcon(android.R.drawable.stat_sys_upload)

    private fun Notification.alertsOnlyOnce() = (flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0
}
