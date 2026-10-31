package fr.free.nrw.commons.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import timber.log.Timber

private const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * A BroadcastReceiver that listens for [DownloadManager.ACTION_DOWNLOAD_COMPLETE].
 * If a download fails, it queries the DownloadManager for the specific failure
 * reason (COLUMN_REASON) and logs it via Timber to aid in future debugging
 * of "Download Unsuccessful" errors.
 */
class DownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val failedDownload = getFailedDownload(context, downloadId) ?: return

        handleFailedDownload(context, failedDownload.title, failedDownload.reason)
    }

    private data class FailedDownload(val title: String, val reason: Int)

    private fun getFailedDownload(context: Context, downloadId: Long): FailedDownload? {
        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

        if (downloadId == -1L || downloadManager == null) return null

        return downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1 &&
                    cursor.getInt(statusIndex) == DownloadManager.STATUS_FAILED
                ) {
                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)

                    FailedDownload(
                        title = (if (titleIndex != -1) cursor.getString(titleIndex) else null) ?: "Unknown",
                        reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun handleFailedDownload(context: Context, title: String, reason: Int) {
        Timber.e("DOWNLOAD FAILED! Title: $title | Reason Code: $reason")

        when (reason) {
            HTTP_TOO_MANY_REQUESTS -> {
                Timber.e("CAUSE: Rate limited (HTTP 429 Too Many Requests)")
                Toast.makeText(
                    context,
                    "Too many downloads in a short time. Please try again in a few seconds.",
                    Toast.LENGTH_LONG
                ).show()
            }
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE ->
                Timber.e("CAUSE: Server rejected request (Likely 403 Forbidden / Missing User-Agent)")
            DownloadManager.ERROR_HTTP_DATA_ERROR ->
                Timber.e("CAUSE: Network connection dropped mid-download (Beta cluster flakiness or VPN)")
            DownloadManager.ERROR_FILE_ERROR ->
                Timber.e("CAUSE: File system rejected the file (Storage issue or invalid filename characters)")
            DownloadManager.ERROR_INSUFFICIENT_SPACE ->
                Timber.e("CAUSE: Device is out of storage space")
            DownloadManager.ERROR_CANNOT_RESUME ->
                Timber.e("CAUSE: Download cannot be resumed")
            else ->
                Timber.e("CAUSE: Unknown system error (Code: $reason)")
        }
    }
}

