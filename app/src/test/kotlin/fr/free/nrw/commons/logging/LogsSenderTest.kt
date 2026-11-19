package fr.free.nrw.commons.logging

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class LogsSenderTest {
    private lateinit var activity: Activity
    private lateinit var logsSender: LogsSender
    private lateinit var zipFile: File

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        logsSender =
            object : LogsSender(mock(SessionManager::class.java)) {
                override fun getExtraInfo(): String = "Test extra info"
            }.apply {
                mailTo = "logs@example.org"
                logFileName = "TestLogs.zip"
                emailSubject = "Test logs"
            }

        File(LogUtils.getLogDirectory(), "CommonsAppLogs.0.log").writeText("A line from the log file\n")
        zipFile = File(LogUtils.getLogZipDirectory(), "TestLogs.zip")
        zipFile.delete()
    }

    @Test
    fun `sending logs from Settings opens an email with the logs attached`() {
        // Settings has no crash report, so it passes null
        logsSender.sendWithNullable(activity, null)

        val chooser = Shadows.shadowOf(activity).nextStartedActivity
        assertNotNull("No email app was opened", chooser)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val email = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull(email)
        assertEquals(Intent.ACTION_SEND, email!!.action)
        assertEquals("Test logs", email.getStringExtra(Intent.EXTRA_SUBJECT))
        assertNotNull(
            "The email has no attachment",
            IntentCompat.getParcelableExtra(email, Intent.EXTRA_STREAM, Uri::class.java),
        )
    }

    @Test
    fun `the attached zip holds the log file and the device info`() {
        logsSender.sendWithNullable(activity, null)

        assertTrue("The zip file was not created", zipFile.exists())
        val names = ZipFile(zipFile).use { zip -> zip.entries().toList().map { it.name } }
        assertTrue("The log file is missing from the zip: $names", "CommonsAppLogs.0.log" in names)
        assertTrue("meta_data.txt is missing from the zip: $names", "meta_data.txt" in names)
    }
}
