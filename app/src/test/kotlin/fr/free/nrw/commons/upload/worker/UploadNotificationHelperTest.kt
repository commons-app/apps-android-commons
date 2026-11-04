package fr.free.nrw.commons.upload.worker

import android.app.Notification
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.Contribution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestCommonsApplication::class)
class UploadNotificationHelperTest {
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationHelper: UploadNotificationHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = mock()
        notificationHelper = UploadNotificationHelper(context)
        notificationHelper.notificationManager = notificationManager
        notificationHelper.currentNotificationTag = "test-tag"
        notificationHelper.currentNotificationID = 1001
    }

    @Test
    fun testGetNotificationBuilder() {
        val builder =
            notificationHelper.getNotificationBuilder(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS)
        assertNotNull(builder)
        val notification = builder.setContentTitle(context.getString(R.string.upload_in_progress)).build()
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
        assertEquals(
            context.getString(R.string.upload_in_progress),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
    }

    @Test
    fun testShowUploadStarted() {
        val contribution = createMockContribution()
        val expectedTag = contribution.localUri.toString()
        val expectedId = (contribution.localUri.toString() + contribution.media.filename).hashCode()

        notificationHelper.showUploadStarted(contribution)

        val captor = argumentCaptor<Notification>()
        verify(notificationManager).notify(eq(expectedTag), eq(expectedId), captor.capture())
        val notification = captor.firstValue

        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
        assertEquals(
            context.getString(
                R.string.upload_progress_notification_title_start,
                contribution.media.displayTitle,
            ),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
    }

    @Test
    fun testUpdateProgress_determinate() {
        val captor = argumentCaptor<Notification>()
        notificationHelper.updateProgress(50L, 100L, "Finishing...", "startingTag", 101)

        verify(notificationManager).cancel("startingTag", 101)
        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertNotNull(notification)
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
    }

    @Test
    fun testUpdateProgress_complete() {
        val captor = argumentCaptor<Notification>()
        notificationHelper.updateProgress(100L, 100L, "Finishing...")

        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals("Finishing...", notification.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
    }

    @Test
    fun testShowSuccessNotification() {
        val contribution = createMockContribution()
        val captor = argumentCaptor<Notification>()
        notificationHelper.showSuccessNotification(contribution)

        assertEquals(Contribution.STATE_COMPLETED, contribution.state)
        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL, notification.channelId)
        assertEquals(
            context.getString(
                R.string.upload_completed_notification_title,
                contribution.media.displayTitle,
            ),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
        assertEquals(
            context.getString(R.string.upload_completed_notification_text),
            notification.extras.getString(NotificationCompat.EXTRA_TEXT),
        )
    }

    @Test
    fun testShowFailedNotification() {
        val contribution = createMockContribution()
        val captor = argumentCaptor<Notification>()
        notificationHelper.showFailedNotification(contribution)

        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL, notification.channelId)
        assertEquals(
            context.getString(
                R.string.upload_failed_notification_title,
                contribution.media.displayTitle,
            ),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
        assertEquals(
            context.getString(R.string.upload_failed_notification_subtitle),
            notification.extras.getString(NotificationCompat.EXTRA_TEXT),
        )
    }

    @Test
    fun testShowErrorNotification_nullErrorInfo() {
        val contribution = createMockContribution()
        contribution.errorInfo = null

        val captor = argumentCaptor<Notification>()
        notificationHelper.showErrorNotification(contribution)

        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL, notification.channelId)
        assertEquals(
            context.getString(
                R.string.upload_failed_notification_title,
                contribution.media.displayTitle,
            ),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
        assertNull(notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT))
    }

    @Test
    fun testShowErrorNotification_withErrorInfo() {
        val contribution = createMockContribution()
        contribution.errorInfo = "Stash upload failed with verification error"

        val captor = argumentCaptor<Notification>()
        notificationHelper.showErrorNotification(contribution)

        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL, notification.channelId)
        assertEquals(
            context.getString(
                R.string.upload_failed_notification_title,
                contribution.media.displayTitle,
            ),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
        assertEquals(
            "Stash upload failed with verification error",
            notification.extras.getString(NotificationCompat.EXTRA_TEXT),
        )
    }

    @Test
    fun testShowInvalidLoginNotification() {
        val contribution = createMockContribution()
        val captor = argumentCaptor<Notification>()
        notificationHelper.showInvalidLoginNotification(contribution)

        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL, notification.channelId)
        assertEquals(
            context.getString(
                R.string.upload_failed_notification_title,
                contribution.media.displayTitle,
            ),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
        assertEquals(
            context.getString(R.string.invalid_login_message),
            notification.extras.getString(NotificationCompat.EXTRA_TEXT),
        )
    }

    @Test
    fun testShowPausedNotification() {
        val contribution = createMockContribution()
        val captor = argumentCaptor<Notification>()
        notificationHelper.showPausedNotification(contribution)

        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
        assertEquals(
            context.getString(
                R.string.upload_paused_notification_title,
                contribution.media.displayTitle,
            ),
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
        assertEquals(
            context.getString(R.string.upload_paused_notification_subtitle),
            notification.extras.getString(NotificationCompat.EXTRA_TEXT),
        )
    }

    @Test
    fun testShowCancelledNotification() {
        val contribution = createMockContribution()
        val captor = argumentCaptor<Notification>()
        notificationHelper.showCancelledNotification(contribution)

        verify(notificationManager).notify(
            eq("test-tag"),
            eq(1001),
            captor.capture(),
        )
        val notification = captor.firstValue
        assertEquals(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS, notification.channelId)
        assertEquals(
            contribution.media.displayTitle,
            notification.extras.getString(NotificationCompat.EXTRA_TITLE),
        )
        assertEquals(
            "Upload has been cancelled!",
            notification.extras.getString(NotificationCompat.EXTRA_TEXT),
        )
    }

    private fun createMockContribution(): Contribution {
        val media: Media = mock()
        whenever(media.pageId).thenReturn("123")
        whenever(media.filename).thenReturn("test.jpg")
        whenever(media.displayTitle).thenReturn("Test Image")
        val uri: Uri = Uri.parse("file:///dummy/path/test.jpg")
        val contribution = Contribution(media = media, pageId = "123", localUri = uri)
        return spy(contribution)
    }
}
