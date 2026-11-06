package fr.free.nrw.commons.media

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MediaReportHelperTest {
    private lateinit var sessionManager: SessionManager
    private lateinit var mediaReportHelper: MediaReportHelper
    private lateinit var context: Context
    private lateinit var media: Media

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = mock()
        media = mock()
        mediaReportHelper = MediaReportHelper(sessionManager)
    }

    @Test
    fun testGetTechInfo_withLoggedInUser() {
        whenever(sessionManager.userName).thenReturn("TestUser")
        whenever(media.imageUrl).thenReturn("https://example.com/test.jpg")
        whenever(media.user).thenReturn("UploaderUser")

        val techInfo = mediaReportHelper.getTechInfo(media, "Copyright violation")

        assertTrue(techInfo.contains("Report type: Copyright violation"))
        assertTrue(techInfo.contains("Image that you want to report: https://example.com/test.jpg"))
        assertTrue(techInfo.contains("User that you want to report: UploaderUser"))
        assertTrue(techInfo.contains("Your username: TestUser"))
        assertTrue(techInfo.contains("Violation reason:"))
    }

    @Test
    fun testGetTechInfo_withLoggedOutUser() {
        whenever(sessionManager.userName).thenReturn(null)
        whenever(media.imageUrl).thenReturn("https://example.com/test.jpg")
        whenever(media.user).thenReturn("UploaderUser")

        val techInfo = mediaReportHelper.getTechInfo(media, "Spam")

        assertTrue(techInfo.contains("Report type: Spam"))
        assertTrue(techInfo.contains("Image that you want to report: https://example.com/test.jpg"))
        assertTrue(techInfo.contains("User that you want to report: UploaderUser"))
        assertFalse(techInfo.contains("Your username:"))
        assertTrue(techInfo.contains("Violation reason:"))
    }

    @Test
    fun testSendReportEmail_success() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        whenever(sessionManager.userName).thenReturn("TestUser")
        whenever(media.imageUrl).thenReturn("https://example.com/test.jpg")
        whenever(media.user).thenReturn("UploaderUser")

        mediaReportHelper.sendReportEmail(activity, media, "Copyright violation")

        val shadowApp = shadowOf(ApplicationProvider.getApplicationContext<TestCommonsApplication>())
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_SENDTO, startedIntent.action)
        assertEquals("mailto:", startedIntent.dataString)
        val emails = startedIntent.getStringArrayExtra(Intent.EXTRA_EMAIL)
        assertNotNull(emails)
        assertEquals(CommonsApplication.REPORT_EMAIL, emails!![0])
        assertEquals(CommonsApplication.REPORT_EMAIL_SUBJECT, startedIntent.getStringExtra(Intent.EXTRA_SUBJECT))
        val text = startedIntent.getStringExtra(Intent.EXTRA_TEXT)
        assertNotNull(text)
        assertTrue(text!!.contains("Report type: Copyright violation"))
    }

    @Test
    fun testSendReportEmail_activityNotFound() {
        val mockContext: Context = mock()
        whenever(mockContext.resources).thenReturn(context.resources)
        whenever(mockContext.applicationContext).thenReturn(context)
        doThrow(ActivityNotFoundException()).whenever(mockContext).startActivity(any())

        // Should handle ActivityNotFoundException gracefully without throwing
        mediaReportHelper.sendReportEmail(mockContext, media, "Copyright violation")
    }

    @Test
    fun testShowReportDialog_nullMedia() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        mediaReportHelper.showReportDialog(activity, null)
        assertNull(ShadowAlertDialog.getLatestDialog())
    }

    @Test
    fun testShowReportDialog_nonNullMedia() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        mediaReportHelper.showReportDialog(activity, media)

        val dialog = ShadowAlertDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }
}
