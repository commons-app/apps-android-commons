package fr.free.nrw.commons.contributions

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.filepicker.FilePicker.onPictureReturnedFromDocuments
import fr.free.nrw.commons.filepicker.FilePicker.openGallery
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.PermissionUtils.PERMISSIONS_STORAGE
import fr.free.nrw.commons.utils.PermissionUtils.checkPermissionsAndPerformActionWithPermissions
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handles gallery and custom-selector selection and gallery-result dispatch.
 */
@Singleton
class ContributionImagePicker
    @Inject
    constructor(
        @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
    ) {
        /**
         * Initiates gallery selection and applies the requested multiple-selection behavior.
         *
         * @param activity activity launching the gallery
         * @param resultLauncher launcher for the selected gallery
         * @param allowMultipleUploads whether the gallery can select multiple images
         */
        fun initiateGalleryPick(
            activity: Activity,
            resultLauncher: ActivityResultLauncher<Intent>,
            allowMultipleUploads: Boolean,
        ) {
            configureContributionFilePicker(activity, defaultKvStore, allowMultipleUploads)
            openGallery(
                activity,
                resultLauncher,
                0,
                defaultKvStore.getBoolean("openDocumentPhotoPickerPref", true),
            )
        }

        /**
         * Initiates custom gallery selection after checking storage permissions.
         *
         * @param activity activity launching the custom selector
         * @param resultLauncher launcher for the selected gallery
         * @param singleSelection whether the selector permits one selected image
         */
        fun initiateCustomGalleryPickWithPermission(
            activity: Activity,
            resultLauncher: ActivityResultLauncher<Intent>,
            singleSelection: Boolean = false,
        ) {
            configureContributionFilePicker(activity, defaultKvStore, true)
            checkPermissionsAndPerformActionWithPermissions(
                activity,
                { FilePicker.openCustomSelector(activity, resultLauncher, 0, singleSelection) },
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale,
                PERMISSIONS_STORAGE.asList(),
            )
        }

        /**
         * Dispatches a gallery activity result to the configured gallery implementation.
         *
         * @param result result returned by the gallery
         * @param activity activity receiving the result
         * @param callbacks callbacks handling the picked image
         */
        fun onPictureReturnedFromGallery(
            result: ActivityResult,
            activity: Activity,
            callbacks: FilePicker.Callbacks,
        ) {
            if (defaultKvStore.getBoolean("openDocumentPhotoPickerPref", true)) {
                onPictureReturnedFromDocuments(result, activity, callbacks)
            } else {
                FilePicker.onPictureReturnedFromGallery(result, activity, callbacks)
            }
        }
    }
