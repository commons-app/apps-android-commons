package fr.free.nrw.commons.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import timber.log.Timber

/**
 * Utility class for managing media file downloads.
 * This class provides a function to start downloading media files to the local SD card/storage.
 * The downloaded file can then be opened in the Gallery or other apps.
 */
object DownloadUtils {

    /**
     * Initiates the downloading of a media file to the device's external storage.
     * This method will check for storage permissions before attempting to download the file.
     * If permissions are granted, the file will be saved to the device's Downloads folder.
     *
     * @param activity Activity context to perform permission checks and display messages
     * @param m Media object representing the file to download
     */
    @JvmStatic
    fun downloadMedia(
        activity: Activity?,
        m: Media,
    ) {
        val imageUrl = m.imageUrl
        var fileName = m.filename
        if (imageUrl == null || fileName == null || activity == null) {
            // Log if any required parameter is null and exit
            Timber.d("Skipping download media as either imageUrl $imageUrl or filename $fileName or activity is null")
            return
        }

        // Strip 'File:' from beginning of filename to ensure it is stored correctly
        fileName = fileName.substringAfter("File:")
        val imageUri = Uri.parse(imageUrl)

        // Prepare a DownloadManager.Request object with the media's URI and other configurations
        val req = try {
            DownloadManager.Request(imageUri).apply {
                setTitle(m.displayTitle)
                setDescription(activity.getString(R.string.app_name))
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                allowScanningByMediaScanner()
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            }
        } catch (e: SecurityException) {
            // Catch SecurityException if storage permission is missing, show a message, and exit
            Toast.makeText(activity, "存储权限被拒绝，无法下载文件", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for storage permissions and perform the download action if granted
        PermissionUtils.checkPermissionsAndPerformAction(
            activity,
            {
                if (activity != null) enqueueRequest(activity, req) // Ensure activity is not null before proceeding
            },
            {
                // Show a message if permissions are denied and download cannot proceed
                Toast.makeText(
                    activity,
                    R.string.download_failed_we_cannot_download_the_file_without_storage_permission,
                    Toast.LENGTH_SHORT,
                ).show()
            },
            R.string.storage_permission, // Title for permission rationale
            R.string.write_storage_permission_rationale, // Message for permission rationale
            *PermissionUtils.PERMISSIONS_STORAGE, // Permissions to request
        )
    }

    /**
     * Enqueues the download request with the system's DownloadManager.
     *
     * @param activity Activity context for accessing the system's DownloadManager
     * @param req DownloadManager.Request configured with the download details
     */
    private fun enqueueRequest(
        activity: Activity,
        req: DownloadManager.Request,
    ) {
        val systemService = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        systemService?.enqueue(req)
    }
}
