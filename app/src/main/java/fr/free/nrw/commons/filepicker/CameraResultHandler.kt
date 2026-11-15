package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.preference.PreferenceManager
import fr.free.nrw.commons.filepicker.FilePicker.Callbacks
import fr.free.nrw.commons.filepicker.FilePicker.ImageSource
import fr.free.nrw.commons.filepicker.FilePicker.KEY_LAST_CAMERA_PHOTO
import fr.free.nrw.commons.filepicker.FilePicker.KEY_PHOTO_URI
import fr.free.nrw.commons.filepicker.PickedFiles.singleFileList
import java.io.File
import java.net.URISyntaxException

/**
 * Handles the result of the camera app: it reads the photo that was taken, reports it through
 * the callbacks and clears the camera state that was stored for the request.
 */
object CameraResultHandler {
    /**
     * Returns the photo saved by the last camera request.
     *
     * @param context The context used to access the shared preferences
     * @return The taken photo, or null if no photo location is stored
     */
    @Throws(URISyntaxException::class)
    @JvmStatic
    private fun takenCameraPicture(context: Context): UploadableFile? {
        val lastCameraPhoto =
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(KEY_LAST_CAMERA_PHOTO, null)
        return if (lastCameraPhoto != null) {
            UploadableFile(File(lastCameraPhoto))
        } else {
            null
        }
    }

    /**
     * Revokes the camera write permission, reports the taken photo through the callbacks (or an
     * error if there is none) and clears the stored camera state.
     *
     * @param activity The activity that started the camera
     * @param callbacks The callbacks that receive the photo or the error
     */
    internal fun processSuccessfulCameraResult(
        activity: Activity,
        callbacks: Callbacks,
    ) {
        revokeLastCameraPermission(activity)

        val photoFile = takenCameraPicture(activity)
        if (photoFile == null) {
            val e = IllegalStateException("Unable to get the picture returned from camera")
            callbacks.onImagePickerError(e, ImageSource.CAMERA_IMAGE, PickedFilesResolver.restoreType(activity))
        } else {
            handleValidCameraPhoto(activity, callbacks, photoFile)
        }

        clearCameraPreferences(activity)
    }

    /**
     * Revokes the write permission on the uri stored for the last camera photo, if there is one.
     *
     * @param activity The activity used to access the preferences and revoke the permission
     */
    private fun revokeLastCameraPermission(activity: Activity) {
        val lastImageUri =
            PreferenceManager
                .getDefaultSharedPreferences(activity)
                .getString(KEY_PHOTO_URI, null)
        if (!lastImageUri.isNullOrEmpty()) {
            revokeWritePermission(activity, Uri.parse(lastImageUri))
        }
    }

    /**
     * Copies the taken photo to the public gallery folder if the configuration asks for it, then
     * reports it through the callbacks.
     *
     * @param activity The activity that started the camera
     * @param callbacks The callbacks that receive the photo
     * @param photoFile The photo that was taken
     */
    private fun handleValidCameraPhoto(
        activity: Activity,
        callbacks: Callbacks,
        photoFile: UploadableFile,
    ) {
        if (FilePicker.configuration(activity).shouldCopyTakenPhotosToPublicGalleryAppFolder()) {
            PickedFilesCopier.copyFilesInSeparateThread(activity, singleFileList(photoFile))
        }
        val files = listOf(photoFile)
        callbacks.onImagesPicked(files, ImageSource.CAMERA_IMAGE, PickedFilesResolver.restoreType(activity))
    }

    /**
     * Removes the stored photo path and uri of the last camera request.
     *
     * @param activity The activity used to access the shared preferences
     */
    private fun clearCameraPreferences(activity: Activity) {
        PreferenceManager
            .getDefaultSharedPreferences(activity)
            .edit()
            .remove(KEY_LAST_CAMERA_PHOTO)
            .remove(FilePicker.KEY_PHOTO_URI)
            .apply()
    }

    /**
     * Revokes the read and write permission that was granted on the given uri.
     *
     * @param context The context used to revoke the permission
     * @param uri The uri to revoke the permission on
     */
    @JvmStatic
    private fun revokeWritePermission(
        context: Context,
        uri: Uri,
    ) {
        context.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
