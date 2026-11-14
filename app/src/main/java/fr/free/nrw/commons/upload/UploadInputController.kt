package fr.free.nrw.commons.upload

import android.content.Intent
import android.location.Location
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.MAX_IMAGE_COUNT
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.UploadActivity.Companion.EXTRA_FILES
import fr.free.nrw.commons.upload.UploadActivity.Companion.IN_APP_CAMERA_UPLOAD
import fr.free.nrw.commons.upload.UploadActivity.Companion.LOCATION_BEFORE_IMAGE_CAPTURE
import fr.free.nrw.commons.utils.PermissionUtils.PERMISSIONS_STORAGE
import fr.free.nrw.commons.utils.PermissionUtils.checkPermissionsAndPerformAction
import fr.free.nrw.commons.utils.PermissionUtils.hasPartialAccess
import fr.free.nrw.commons.utils.PermissionUtils.hasPermission
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import timber.log.Timber

/** Reads shared uploads, requests permissions, and selects camera location. */
internal class UploadInputController {
    lateinit var view: UploadActivityView
    private val activity get() = view.activity

    /** Requests storage access before receiving internally selected files. */
    fun checkStoragePermissions() {
        // The share intent provides files via content uris with temporary read permissions,
        // so we do not need to obtain storage permissions
        val action = activity.intent.action
        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) {
            // Get the external items first
            view.input.receiveExternalSharedItems()
            view.steps.receiveSharedItems()
            return
        }

        // Check if all required permissions are granted
        val hasAllPermissions = hasPermission(activity, PERMISSIONS_STORAGE)
        val hasPartialAccess = hasPartialAccess(activity)

        if (hasAllPermissions || hasPartialAccess) {
            // All required permissions are granted, so enable UI elements and perform actions
            view.steps.receiveSharedItems()
            view.binding.cvContainerTopCard.visibility = View.VISIBLE
        } else {
            // Permissions are missing
            view.binding.cvContainerTopCard.visibility = View.INVISIBLE
            if (view.isShowPermissionsDialog) {
                checkPermissionsAndPerformAction(
                    activity,
                    Runnable {
                        view.binding.cvContainerTopCard.visibility = View.VISIBLE
                        view.steps.receiveSharedItems()
                    },
                    Runnable {
                        view.isShowPermissionsDialog = true
                        view.input.checkStoragePermissions()
                    },
                    R.string.storage_permission_title,
                    R.string.write_storage_permission_rationale_for_image_share,
                    permissions = PERMISSIONS_STORAGE,
                )
            }
        }
        /* If all permissions are not granted and a dialog is already showing on screen
         showPermissionsDialog will set to false making it not show dialog again onResume,
         but if user Denies any permission showPermissionsDialog will be to true
         and permissions dialog will be shown again.
         */
        view.isShowPermissionsDialog = hasAllPermissions
    }

    /**
     * Calculate the difference between current location and
     * location recorded before capturing the image
     *
     */
    fun getLocationDifference(
        currentLocation: LatLng,
        previousLocation: LatLng?,
    ): Float {
        if (previousLocation == null) {
            return 0.0f
        }
        val distance = FloatArray(2)
        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            previousLocation.latitude,
            previousLocation.longitude,
            distance,
        )
        return distance[0]
    }

    /** Receives files shared by another application. */
    fun receiveExternalSharedItems() {
        val result = activity.contributionController!!.handleExternalImagesPicked(activity, activity.intent)
        // enforce the limit for the external sharing of the file
        view.uploadableFiles = view.input.limitToMaxImageCount(result.files)
        view.skippedSharedFilesCount = result.skippedCount
    }

    /**
     * Keeps only the first [MAX_IMAGE_COUNT] files and tells the user when some were dropped.
     */
    fun limitToMaxImageCount(files: List<UploadableFile>): MutableList<UploadableFile> {
        if (files.size <= MAX_IMAGE_COUNT) {
            return files.toMutableList()
        }
        showLongToast(activity, activity.getString(R.string.upload_limit_exceeded, MAX_IMAGE_COUNT))
        return files.take(MAX_IMAGE_COUNT).toMutableList()
    }

    /** Reads files and camera metadata from an internal upload intent. */
    fun receiveInternalSharedItems() {
        Timber.d("Intent has EXTRA_FILES: ${EXTRA_FILES}")
        val parsedFiles =
            readIntentExtra("Error reading files from intent", mutableListOf<UploadableFile>()) {
                // Check if intent has the extra before trying to read it
                if (!activity.intent.hasExtra(EXTRA_FILES)) {
                    Timber.w("No EXTRA_FILES found in intent")
                    mutableListOf()
                } else {
                    // Try to get the files as Parcelable array
                    val files =
                        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                            activity.intent.getParcelableArrayListExtra(EXTRA_FILES, UploadableFile::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            activity.intent.getParcelableArrayListExtra<UploadableFile>(EXTRA_FILES)
                        }

                    // Convert to mutable list or return empty list if null
                    files?.toMutableList() ?: run {
                        Timber.w("Files array was null")
                        mutableListOf()
                    }
                }
            }

        // trim the list if it exceeds the MAX_IMAGE_COUNT
        view.uploadableFiles = view.input.limitToMaxImageCount(parsedFiles)
        view.isMultipleFilesSelected = view.uploadableFiles.size > 1
        Timber.i("Received files count: ${view.uploadableFiles.size}")
        view.uploadableFiles.forEachIndexed { index, file ->
            Timber.d("File $index path: ${file.getFilePath()}")
        }

        // Handle other extras with null safety
        view.place =
            readIntentExtra<Place?>("Error reading place", null) {
                if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                    activity.intent.getParcelableExtra(PLACE_OBJECT, Place::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    activity.intent.getParcelableExtra(PLACE_OBJECT)
                }
            }

        view.prevLocation =
            readIntentExtra<LatLng?>("Error reading location", null) {
                if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                    activity.intent.getParcelableExtra(LOCATION_BEFORE_IMAGE_CAPTURE, LatLng::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    activity.intent.getParcelableExtra(LOCATION_BEFORE_IMAGE_CAPTURE)
                }
            }

        view.isInAppCameraUpload = activity.intent.getBooleanExtra(IN_APP_CAMERA_UPLOAD, false)
        activity.resetDirectPrefs()
    }

    /**
     * Handle null URI from the received intent.
     * Current implementation will simply show a toast and finish the upload activity.
     */
    fun handleNullMedia() {
        showLongToast(activity, R.string.error_processing_image)
        activity.finish()
    }

    /**
     * If the permission for Location is turned on and certain
     * conditions are met, returns current location of the user.
     */
    fun handleLocation() {
        val locationPermissionsHelper =
            LocationPermissionsHelper(
                activity,
                activity.locationManager!!,
                null,
            )
        if (locationPermissionsHelper.isLocationAccessToAppsTurnedOn()) {
            view.currLocation = activity.locationManager!!.getLastLocation()
        }

        if (view.currLocation != null) {
            val locationDifference = view.input.getLocationDifference(view.currLocation!!, view.prevLocation)
            val isLocationTagUnchecked = view.isLocationTagUncheckedInTheSettings
            val isCameraLocationEnabled =
                activity.defaultKvStore.getBoolean("inAppCameraLocationPref") && view.isInAppCameraUpload
            /* Remove location if the user has unchecked the Location EXIF tag in the
                       Manage EXIF Tags setting or turned "Record location for in-app shots" off.
                       Also, location information is discarded if the difference between
                       current location and location recorded just before capturing the image
                       is greater than 100 meters */
            if (isLocationTagUnchecked ||
                locationDifference > 100 ||
                !isCameraLocationEnabled
            ) {
                view.currLocation = null
            }
        }
    }

    /** Logs malformed extras and preserves their defaults without swallowing fatal errors. */
    private fun <T> readIntentExtra(
        message: String,
        fallback: T,
        read: () -> T,
    ): T =
        runCatching(read).getOrElse { error ->
            if (error !is Exception) {
                throw error
            }
            Timber.e(error, message)
            fallback
        }
}
