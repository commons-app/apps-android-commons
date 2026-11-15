package fr.free.nrw.commons.utils

import android.app.DownloadManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.database.Cursor
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class DownloadReceiverTest {
    private lateinit var downloadReceiver: DownloadReceiver
    private lateinit var context: Context
    private lateinit var downloadManager: DownloadManager
    private lateinit var intent: Intent
    private lateinit var cursor: Cursor

    private class TestContext(
        base: Context,
        var customDownloadManager: DownloadManager?,
    ) : ContextWrapper(base) {
        override fun getSystemService(name: String): Any? {
            if (Context.DOWNLOAD_SERVICE == name) {
                return customDownloadManager
            }
            return super.getSystemService(name)
        }
    }

    private lateinit var testContext: TestContext

    @Before
    fun setUp() {
        downloadReceiver = DownloadReceiver()
        downloadManager = mock()
        intent = mock()
        cursor = mock()

        val baseContext: Context = ApplicationProvider.getApplicationContext()
        testContext = TestContext(baseContext, downloadManager)
        context = testContext
    }

    private fun setupFailedCursor(
        reason: Int,
        title: String? = "Test File",
    ) {
        whenever(intent.action).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        whenever(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)).thenReturn(123L)
        whenever(downloadManager.query(any())).thenReturn(cursor)
        whenever(cursor.moveToFirst()).thenReturn(true)
        whenever(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)).thenReturn(0)
        whenever(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_FAILED)
        whenever(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)).thenReturn(1)
        whenever(cursor.getInt(1)).thenReturn(reason)
        whenever(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)).thenReturn(2)
        whenever(cursor.getString(2)).thenReturn(title)
    }

    @Test
    fun testOnReceiveWithDifferentActionDoesNothing() {
        whenever(intent.action).thenReturn("SOME_OTHER_ACTION")

        downloadReceiver.onReceive(context, intent)

        verify(downloadManager, never()).query(any())
    }

    @Test
    fun testOnReceiveWithInvalidDownloadIdDoesNothing() {
        whenever(intent.action).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        whenever(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)).thenReturn(-1L)

        downloadReceiver.onReceive(context, intent)

        verify(downloadManager, never()).query(any())
    }

    @Test
    fun testOnReceiveWhenDownloadManagerIsNullDoesNotCrash() {
        whenever(intent.action).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        whenever(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)).thenReturn(123L)
        testContext.customDownloadManager = null

        downloadReceiver.onReceive(context, intent)

        verify(downloadManager, never()).query(any())
    }

    @Test
    fun testOnReceiveWhenCursorIsNullDoesNotCrash() {
        whenever(intent.action).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        whenever(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)).thenReturn(123L)
        whenever(downloadManager.query(any())).thenReturn(null)

        downloadReceiver.onReceive(context, intent)

        verify(cursor, never()).moveToFirst()
    }

    @Test
    fun testOnReceiveWhenCursorIsEmptyDoesNothing() {
        whenever(intent.action).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        whenever(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)).thenReturn(123L)
        whenever(downloadManager.query(any())).thenReturn(cursor)
        whenever(cursor.moveToFirst()).thenReturn(false)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsSuccessfulDoesNotHandleFailure() {
        whenever(intent.action).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        whenever(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)).thenReturn(123L)
        whenever(downloadManager.query(any())).thenReturn(cursor)
        whenever(cursor.moveToFirst()).thenReturn(true)
        whenever(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)).thenReturn(0)
        whenever(cursor.getInt(0)).thenReturn(DownloadManager.STATUS_SUCCESSFUL)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsFailedWithRateLimitError() {
        setupFailedCursor(429, "Test File")

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsFailedWithUnhandledHttpCode() {
        setupFailedCursor(DownloadManager.ERROR_UNHANDLED_HTTP_CODE, null)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsFailedWithHttpDataError() {
        setupFailedCursor(DownloadManager.ERROR_HTTP_DATA_ERROR)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsFailedWithFileError() {
        setupFailedCursor(DownloadManager.ERROR_FILE_ERROR)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsFailedWithInsufficientSpace() {
        setupFailedCursor(DownloadManager.ERROR_INSUFFICIENT_SPACE)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsFailedWithCannotResume() {
        setupFailedCursor(DownloadManager.ERROR_CANNOT_RESUME)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }

    @Test
    fun testOnReceiveWhenDownloadStatusIsFailedWithUnknownError() {
        setupFailedCursor(9999)

        downloadReceiver.onReceive(context, intent)

        verify(cursor).close()
    }
}
