package fr.free.nrw.commons.filepicker

import android.content.Context
import androidx.preference.PreferenceManager

class FilePickerConfiguration(
    private val context: Context,
) : Constants {
    fun setAllowMultiplePickInGallery(allowMultiple: Boolean): FilePickerConfiguration {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(Constants.BundleKeys.ALLOW_MULTIPLE, allowMultiple)
            .apply()
        return this
    }

    fun setCopyTakenPhotosToPublicGalleryAppFolder(copy: Boolean): FilePickerConfiguration {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(Constants.BundleKeys.COPY_TAKEN_PHOTOS, copy)
            .apply()
        return this
    }

    fun getFolderName(): String =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(
                Constants.BundleKeys.FOLDER_NAME,
                Constants.DEFAULT_FOLDER_NAME,
            ) ?: Constants.DEFAULT_FOLDER_NAME

    fun allowsMultiplePickingInGallery(): Boolean =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean(Constants.BundleKeys.ALLOW_MULTIPLE, false)

    fun shouldCopyTakenPhotosToPublicGalleryAppFolder(): Boolean =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean(Constants.BundleKeys.COPY_TAKEN_PHOTOS, false)

    fun shouldCopyPickedImagesToPublicGalleryAppFolder(): Boolean =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean(Constants.BundleKeys.COPY_PICKED_IMAGES, false)
}
