package fr.free.nrw.commons.utils

import android.Manifest.permission
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import timber.log.Timber

object DownloadUtils {
    /**
     * Start the media file downloading to the local SD card/storage. The file can then be opened in
     * Gallery or other apps.
     *
     * @param m Media file to download
     */
    @JvmStatic
    fun downloadMedia(activity: Activity?, m: Media) {
        val imageUrl = m.imageUrl
        var fileName = m.filename
        if (imageUrl == null || fileName == null || activity == null) {
            Timber.d("Skipping download media as either imageUrl $imageUrl or filename $fileName activity is null")
            return
        }
        // Strip 'File:' from beginning of filename, we really shouldn't store it
        fileName = fileName.substringAfter("File:")
        val imageUri = Uri.parse(imageUrl)
        val req = DownloadManager.Request(imageUri).apply {
            setTitle(m.displayTitle)
            setDescription(activity.getString(R.string.app_name))
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            allowScanningByMediaScanner()
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        PermissionUtils.checkPermissionsAndPerformAction(
            activity,
            { enqueueRequest(activity, req) },
            {
                Toast.makeText(
                    activity,
                    R.string.download_failed_we_cannot_download_the_file_without_storage_permission,
                    Toast.LENGTH_SHORT
                ).show()
            },
            R.string.storage_permission,
            R.string.write_storage_permission_rationale,
            *PermissionUtils.PERMISSIONS_STORAGE
            )
    }

    private fun enqueueRequest(activity: Activity, req: DownloadManager.Request) {
        val systemService =
            activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        systemService?.enqueue(req)
    }
}