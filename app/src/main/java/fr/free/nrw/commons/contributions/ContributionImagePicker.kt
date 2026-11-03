package fr.free.nrw.commons.contributions

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.DefaultCallback
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.filepicker.FilePicker.HandleActivityResult
import fr.free.nrw.commons.filepicker.FilePicker.configuration
import fr.free.nrw.commons.filepicker.FilePicker.onPictureReturnedFromDocuments
import fr.free.nrw.commons.filepicker.FilePicker.openCameraForImage
import fr.free.nrw.commons.filepicker.FilePicker.openGallery
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.PermissionUtils.PERMISSIONS_STORAGE
import fr.free.nrw.commons.utils.PermissionUtils.checkPermissionsAndPerformActionWithPermissions
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Coordinates camera and gallery selection, including permission handling and upload handoff.
 */
@Singleton
class ContributionImagePicker
    @Inject
    constructor(
        @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
    ) {
        private var locationBeforeImageCapture: LatLng? = null
        private var isInAppCameraUpload = false

        // Flag to track if an internal upload is in progress.
        var isInternalUploadInProgress = false

        @JvmField
        var locationPermissionCallback: LocationPermissionCallback? = null
        private var locationPermissionsHelper: LocationPermissionsHelper? = null

        @JvmField
        @Inject
        var locationManager: LocationServiceManager? = null

        /**
         * Check for permissions and initiate camera click
         */
        fun initiateCameraPick(
            activity: Activity,
            inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            val useExtStorage = defaultKvStore.getBoolean("useExternalStorage", true)
            if (!useExtStorage) {
                initiateCameraUpload(activity, resultLauncher)
                return
            }

            checkPermissionsAndPerformActionWithPermissions(
                activity,
                {
                    if (defaultKvStore.getBoolean("inAppCameraFirstRun")) {
                        defaultKvStore.putBoolean("inAppCameraFirstRun", false)
                        askUserToAllowLocationAccess(
                            activity,
                            inAppCameraLocationPermissionLauncher,
                            resultLauncher,
                        )
                    } else if (defaultKvStore.getBoolean("inAppCameraLocationPref")) {
                        createDialogsAndHandleLocationPermissions(
                            activity,
                            inAppCameraLocationPermissionLauncher,
                            resultLauncher,
                        )
                    } else {
                        initiateCameraUpload(activity, resultLauncher)
                    }
                },
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale,
                PERMISSIONS_STORAGE.asList(),
            )
        }

        /**
         * Asks users to provide location access
         *
         * @param activity
         */
        private fun createDialogsAndHandleLocationPermissions(
            activity: Activity,
            inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>?,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            locationPermissionCallback =
                object : LocationPermissionCallback {
                    /**
                     * Shows the supplied denial message as a long toast, then initiates camera upload.
                     */
                    override fun onLocationPermissionDenied(toastMessage: String) {
                        Toast
                            .makeText(
                                activity,
                                toastMessage,
                                Toast.LENGTH_LONG,
                            ).show()
                        initiateCameraUpload(activity, resultLauncher)
                    }

                    /**
                     * If location is unavailable, shows the location-off dialog and initiates camera upload from its
                     * callback. Otherwise, initiates camera upload immediately.
                     */
                    override fun onLocationPermissionGranted() {
                        if (!locationPermissionsHelper!!.isLocationAccessToAppsTurnedOn()) {
                            locationPermissionsHelper!!.showLocationOffDialog(
                                activity,
                                R.string.in_app_camera_needs_location,
                                R.string.in_app_camera_location_unavailable,
                            ) {
                                initiateCameraUpload(activity, resultLauncher)
                            }
                        } else {
                            initiateCameraUpload(activity, resultLauncher)
                        }
                    }
                }

            locationPermissionsHelper =
                LocationPermissionsHelper(
                    activity,
                    locationManager!!,
                    locationPermissionCallback,
                )
            inAppCameraLocationPermissionLauncher?.launch(
                arrayOf(permission.ACCESS_FINE_LOCATION),
            )
        }

        /**
         * Handles the camera location permission rationale flow.
         */
        fun handleShowRationaleFlowCameraLocation(
            activity: Activity,
            inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>?,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            showAlertDialog(
                activity,
                activity.getString(R.string.location_permission_title),
                activity.getString(R.string.in_app_camera_location_permission_rationale),
                activity.getString(R.string.ok),
                activity.getString(R.string.cancel),
                {
                    createDialogsAndHandleLocationPermissions(
                        activity,
                        inAppCameraLocationPermissionLauncher,
                        resultLauncher,
                    )
                },
                {
                    locationPermissionCallback!!.onLocationPermissionDenied(
                        activity.getString(R.string.in_app_camera_location_permission_denied),
                    )
                },
                null,
            )
        }

        /**
         * Suggest user to attach location information with pictures. If the user selects "Yes", then:
         *
         *
         * Location is taken from the EXIF if the default camera application does not redact location
         * tags.
         *
         *
         * Otherwise, if the EXIF metadata does not have location information, then location captured by
         * the app is used
         *
         * @param activity
         */
        private fun askUserToAllowLocationAccess(
            activity: Activity,
            inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            showAlertDialog(
                activity,
                activity.getString(R.string.in_app_camera_location_permission_title),
                activity.getString(R.string.in_app_camera_location_access_explanation),
                activity.getString(R.string.option_allow),
                activity.getString(R.string.option_dismiss),
                {
                    defaultKvStore.putBoolean("inAppCameraLocationPref", true)
                    createDialogsAndHandleLocationPermissions(
                        activity,
                        inAppCameraLocationPermissionLauncher,
                        resultLauncher,
                    )
                },
                {
                    showLongToast(activity, R.string.in_app_camera_location_permission_denied)
                    defaultKvStore.putBoolean("inAppCameraLocationPref", false)
                    initiateCameraUpload(activity, resultLauncher)
                },
                null,
            )
        }

        /**
         * Initiate gallery picker
         */
        fun initiateGalleryPick(
            activity: Activity,
            resultLauncher: ActivityResultLauncher<Intent>,
            allowMultipleUploads: Boolean,
        ) {
            setPickerConfiguration(activity, allowMultipleUploads)
            openGallery(
                activity,
                resultLauncher,
                0,
                defaultKvStore.getBoolean("openDocumentPhotoPickerPref", true),
            )
        }

        /**
         * Initiate gallery picker with permission
         */
        fun initiateCustomGalleryPickWithPermission(
            activity: Activity,
            resultLauncher: ActivityResultLauncher<Intent>,
            singleSelection: Boolean = false,
        ) {
            setPickerConfiguration(activity, true)

            checkPermissionsAndPerformActionWithPermissions(
                activity,
                { FilePicker.openCustomSelector(activity, resultLauncher, 0, singleSelection) },
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale,
                PERMISSIONS_STORAGE.asList(),
            )
        }

        /**
         * Sets configuration for file picker
         */
        private fun setPickerConfiguration(
            activity: Activity,
            allowMultipleUploads: Boolean,
        ) {
            val copyToExternalStorage = defaultKvStore.getBoolean("useExternalStorage", true)
            configuration(activity)
                .setCopyTakenPhotosToPublicGalleryAppFolder(copyToExternalStorage)
                .setAllowMultiplePickInGallery(allowMultipleUploads)
        }

        /**
         * Initiate camera upload by opening camera
         */
        private fun initiateCameraUpload(
            activity: Activity,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            setPickerConfiguration(activity, false)
            if (defaultKvStore.getBoolean("inAppCameraLocationPref", false)) {
                locationBeforeImageCapture = locationManager!!.getLastLocation()
            }
            isInAppCameraUpload = true
            openCameraForImage(activity, resultLauncher, 0)
        }

        /**
         * Handles images returned from the gallery picker.
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

        /**
         * Attaches callback for file picker.
         *
         * On cancellation it resets the internal-upload flag and removes the nearby-place preference;
         * on error it shows a toast; on success it builds the upload intent and starts the activity.
         */
        fun handleActivityResultWithCallback(
            activity: Activity,
            handleActivityResult: HandleActivityResult,
        ) {
            handleActivityResult.onHandleActivityResult(
                object : DefaultCallback() {
                    /**
                     * Resets upload state and removes the stored nearby place after cancellation.
                     */
                    override fun onCanceled(
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        super.onCanceled(source, type)
                        isInternalUploadInProgress = false // reset flag on cancel
                        defaultKvStore.remove(PLACE_OBJECT)
                    }

                    /**
                     * Shows an error toast when the image picker reports an error.
                     */
                    override fun onImagePickerError(
                        e: Exception,
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        showShortToast(activity, R.string.error_occurred_in_picking_images)
                    }

                    /**
                     * Builds the upload intent with selected files and available place and camera
                     * data, resets the camera-upload flag, then starts the upload activity.
                     */
                    override fun onImagesPicked(
                        imagesFiles: List<UploadableFile>,
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        val shareIntent = Intent(activity, UploadActivity::class.java)
                        shareIntent.setAction(ContributionController.ACTION_INTERNAL_UPLOADS)
                        shareIntent
                            .putParcelableArrayListExtra(
                                UploadActivity.EXTRA_FILES,
                                ArrayList(imagesFiles),
                            )
                        val place = defaultKvStore.getJson<Place>(PLACE_OBJECT, Place::class.java)

                        if (place != null) {
                            shareIntent.putExtra(PLACE_OBJECT, place)
                        }

                        if (locationBeforeImageCapture != null) {
                            shareIntent.putExtra(
                                UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE,
                                locationBeforeImageCapture,
                            )
                        }

                        shareIntent.putExtra(
                            UploadActivity.IN_APP_CAMERA_UPLOAD,
                            isInAppCameraUpload,
                        )
                        isInAppCameraUpload = false // reset the flag for next use
                        activity.startActivity(shareIntent)
                    }
                },
            )
        }
    }
