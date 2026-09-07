package fr.free.nrw.commons.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import timber.log.Timber

//Added Javadocs for DownloadReceiver
/**
 * A BroadcastReceiver that listens for [DownloadManager.ACTION_DOWNLOAD_COMPLETE].
 * If a download fails, it queries the DownloadManager for the specific failure
 * reason (COLUMN_REASON) and logs it via Timber to aid in future debugging
 * of "Download Unsuccessful" errors.
 */
class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {

            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == -1L) return

            val query = DownloadManager.Query().setFilterById(downloadId)
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)

                val status = cursor.getInt(statusIndex)

                if (status == DownloadManager.STATUS_FAILED) {
                    val reason = cursor.getInt(reasonIndex)
                    val title = if (titleIndex != -1) cursor.getString(titleIndex) else "Unknown"

                    Timber.e("DOWNLOAD FAILED! Title: $title | Reason Code: $reason")

                    if (reason == 429) {
                        Timber.e("CAUSE: Rate limited (HTTP 429 Too Many Requests)")
                        Toast.makeText(
                            context,
                            "Too many downloads. Please wait a moment.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        when (reason) {
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
                cursor.close()
            }
        }
    }
}