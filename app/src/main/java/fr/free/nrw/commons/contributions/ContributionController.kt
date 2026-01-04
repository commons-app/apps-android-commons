package fr.free.nrw.commons.contributions

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.DefaultCallback
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.filepicker.FilePicker.HandleActivityResult
import fr.free.nrw.commons.filepicker.FilePicker.configuration
import fr.free.nrw.commons.filepicker.FilePicker.handleExternalImagesPicked
import fr.free.nrw.commons.filepicker.FilePicker.onPictureReturnedFromDocuments
import fr.free.nrw.commons.filepicker.FilePicker.openCameraForImage
import fr.free.nrw.commons.filepicker.FilePicker.openCustomSelector
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
import fr.free.nrw.commons.utils.PermissionUtils.checkPermissionsAndPerformAction
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ContributionController @Inject constructor(@param:Named("default_preferences") private val defaultKvStore: JsonKvStore) {
    private var locationBeforeImageCapture: LatLng? = null
    private var isInAppCameraUpload = false
    @JvmField
    var locationPermissionCallback: LocationPermissionCallback? = null
    private var locationPermissionsHelper: LocationPermissionsHelper? = null

    // Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
    // LiveData<PagedList<Contribution>> failedAndPendingContributionList;
    @JvmField
    var pendingContributionList: LiveData<PagedList<Contribution>>? = null
    @JvmField
    var failedContributionList: LiveData<PagedList<Contribution>>? = null

    @JvmField
    @Inject
    var locationManager: LocationServiceManager? = null

    @JvmField
    @Inject
    var repository: ContributionsRepository? = null

    /**
     * Check for permissions and initiate camera click
     */
    fun initiateCameraPick(
        activity: Activity,
        inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>,
        resultLauncher: ActivityResultLauncher<Intent>
    ) {
        val useExtStorage = defaultKvStore.getBoolean("useExternalStorage", true)
        if (!useExtStorage) {
            initiateCameraUpload(activity, resultLauncher)
            return
        }

        checkPermissionsAndPerformAction(
            activity,
            {
                if (defaultKvStore.getBoolean("inAppCameraFirstRun")) {
                    defaultKvStore.putBoolean("inAppCameraFirstRun", false)
                    askUserToAllowLocationAccess(
                        activity,
                        inAppCameraLocationPermissionLauncher,
                        resultLauncher
                    )
                } else if (defaultKvStore.getBoolean("inAppCameraLocationPref")) {
                    createDialogsAndHandleLocationPermissions(
                        activity,
                        inAppCameraLocationPermissionLauncher, resultLauncher
                    )
                } else {
                    initiateCameraUpload(activity, resultLauncher)
                }
            },
            R.string.storage_permission_title,
            R.string.write_storage_permission_rationale,
            *PERMISSIONS_STORAGE
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
        resultLauncher: ActivityResultLauncher<Intent>
    ) {
        locationPermissionCallback = object : LocationPermissionCallback {
            override fun onLocationPermissionDenied(toastMessage: String) {
                showLongToast(activity, toastMessage)
                initiateCameraUpload(activity, resultLauncher)
            }

            override fun onLocationPermissionGranted() {
                if (!locationPermissionsHelper!!.isLocationAccessToAppsTurnedOn()) {
                    locationPermissionsHelper!!.showLocationOffDialog(
                        activity, R.string.in_app_camera_needs_location
                    )
                } else {
                    initiateCameraUpload(activity, resultLauncher)
                }
            }

            // Fix:impleement the new callback method
            override fun onLocationServiceUnAvailable() {
                showLongToast(activity, R.string.in_app_camera_location_unavailable)
                initiateCameraUpload(activity, resultLauncher)
            }
        }

        locationPermissionsHelper = LocationPermissionsHelper(
            activity, locationManager!!, locationPermissionCallback
        )
        inAppCameraLocationPermissionLauncher?.launch(
            arrayOf(permission.ACCESS_FINE_LOCATION)
        )
    }

    /**
     * Shows a dialog alerting the user about location services being off and asking them to turn it
     * on
     * TODO: Add a seperate callback in LocationPermissionsHelper for this.
     * Ref: https://github.com/commons-app/apps-android-commons/pull/5494/files#r1510553114
     *
     * @param activity           Activity reference
     * @param dialogTextResource Resource id of text to be shown in dialog
     * @param toastTextResource  Resource id of text to be shown in toast
     * @param resultLauncher
     */
    private fun showLocationOffDialog(
        activity: Activity, dialogTextResource: Int,
        toastTextResource: Int, resultLauncher: ActivityResultLauncher<Intent>
    ) {
        showAlertDialog(activity,
            activity.getString(R.string.ask_to_turn_location_on),
            activity.getString(dialogTextResource),
            activity.getString(R.string.title_app_shortcut_setting),
            activity.getString(R.string.cancel),
            { locationPermissionsHelper!!.openLocationSettings(activity) },
            {
                Toast.makeText(
                    activity, activity.getString(toastTextResource),
                    Toast.LENGTH_LONG
                ).show()
                initiateCameraUpload(activity, resultLauncher)
            }
        )
    }

    fun handleShowRationaleFlowCameraLocation(
        activity: Activity,
        inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>?,
        resultLauncher: ActivityResultLauncher<Intent>
    ) {
        showAlertDialog(
            activity, activity.getString(R.string.location_permission_title),
            activity.getString(R.string.in_app_camera_location_permission_rationale),
            activity.getString(R.string.ok),
            activity.getString(R.string.cancel),
            {
                createDialogsAndHandleLocationPermissions(
                    activity,
                    inAppCameraLocationPermissionLauncher, resultLauncher
                )
            },
            {
                locationPermissionCallback!!.onLocationPermissionDenied(
                    activity.getString(R.string.in_app_camera_location_permission_denied)
                )
            },
            null
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
        resultLauncher: ActivityResultLauncher<Intent>
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
                    inAppCameraLocationPermissionLauncher, resultLauncher
                )
            },
            {
                showLongToast(activity, R.string.in_app_camera_location_permission_denied)
                defaultKvStore.putBoolean("inAppCameraLocationPref", false)
                initiateCameraUpload(activity, resultLauncher)
            },
            null
        )
    }

    /**
     * Initiate gallery picker
     */
    fun initiateGalleryPick(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<Intent>,
        allowMultipleUploads: Boolean
    ) {
        initiateGalleryUpload(activity, resultLauncher, allowMultipleUploads)
    }

    /**
     * Initiate gallery picker with permission
     */
    fun initiateCustomGalleryPickWithPermission(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<Intent>,
        singleSelection: Boolean = false
    ) {
        setPickerConfiguration(activity, true)

        checkPermissionsAndPerformAction(
            activity,
            { FilePicker.openCustomSelector(activity, resultLauncher, 0, singleSelection) },
            R.string.storage_permission_title,
            R.string.write_storage_permission_rationale,
            *PERMISSIONS_STORAGE
        )
    }


    /**
     * Open chooser for gallery uploads
     */
    private fun initiateGalleryUpload(
        activity: Activity, resultLauncher: ActivityResultLauncher<Intent>,
        allowMultipleUploads: Boolean
    ) {
        setPickerConfiguration(activity, allowMultipleUploads)
        openGallery(activity, resultLauncher, 0, isDocumentPhotoPickerPreferred)
    }

    /**
     * Sets configuration for file picker
     */
    private fun setPickerConfiguration(
        activity: Activity,
        allowMultipleUploads: Boolean
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
        resultLauncher: ActivityResultLauncher<Intent>
    ) {
        setPickerConfiguration(activity, false)
        if (defaultKvStore.getBoolean("inAppCameraLocationPref", false)) {
            locationBeforeImageCapture = locationManager!!.getLastLocation()
        }
        isInAppCameraUpload = true
        openCameraForImage(activity, resultLauncher, 0)
    }

    private val isDocumentPhotoPickerPreferred: Boolean
        get() = defaultKvStore.getBoolean(
            "openDocumentPhotoPickerPref", true
        )

    fun onPictureReturnedFromGallery(
        result: ActivityResult,
        activity: Activity,
        callbacks: FilePicker.Callbacks
    ) {
        if (isDocumentPhotoPickerPreferred) {
            onPictureReturnedFromDocuments(result, activity, callbacks)
        } else {
            FilePicker.onPictureReturnedFromGallery(result, activity, callbacks)
        }
    }

    fun onPictureReturnedFromCustomSelector(
        result: ActivityResult,
        activity: Activity,
        callbacks: FilePicker.Callbacks
    ) {
        FilePicker.onPictureReturnedFromCustomSelector(result, activity, callbacks)
    }

    fun onPictureReturnedFromCamera(
        result: ActivityResult,
        activity: Activity,
        callbacks: FilePicker.Callbacks
    ) {
        FilePicker.onPictureReturnedFromCamera(result, activity, callbacks)
    }

    /**
     * Attaches callback for file picker.
     */
    fun handleActivityResultWithCallback(
        activity: Activity,
        handleActivityResult: HandleActivityResult
    ) {
        handleActivityResult.onHandleActivityResult(object : DefaultCallback() {
            override fun onCanceled(source: FilePicker.ImageSource, type: Int) {
                super.onCanceled(source, type)
                defaultKvStore.remove(PLACE_OBJECT)
            }

            override fun onImagePickerError(
                e: Exception, source: FilePicker.ImageSource,
                type: Int
            ) {
                showShortToast(activity, R.string.error_occurred_in_picking_images)
            }

            override fun onImagesPicked(
                imagesFiles: List<UploadableFile>,
                source: FilePicker.ImageSource, type: Int
            ) {
                val intent = handleImagesPicked(activity, imagesFiles)
                activity.startActivity(intent)
            }
        })
    }

    fun handleExternalImagesPicked(
        activity: Activity,
        data: Intent?
    ): List<UploadableFile> {
        return handleExternalImagesPicked(data, activity)
    }

    /**
     * Returns intent to be passed to upload activity Attaches place object for nearby uploads and
     * location before image capture if in-app camera is used
     */
    private fun handleImagesPicked(
        context: Context,
        imagesFiles: List<UploadableFile>
    ): Intent {
        val shareIntent = Intent(context, UploadActivity::class.java)
        shareIntent.setAction(ACTION_INTERNAL_UPLOADS)
        shareIntent
            .putParcelableArrayListExtra(UploadActivity.EXTRA_FILES, ArrayList(imagesFiles))
        val place = defaultKvStore.getJson<Place>(PLACE_OBJECT, Place::class.java)

        if (place != null) {
            shareIntent.putExtra(PLACE_OBJECT, place)
        }

        if (locationBeforeImageCapture != null) {
            shareIntent.putExtra(
                UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE,
                locationBeforeImageCapture
            )
        }

        shareIntent.putExtra(
            UploadActivity.IN_APP_CAMERA_UPLOAD,
            isInAppCameraUpload
        )
        isInAppCameraUpload = false // reset the flag for next use
        return shareIntent
    }

    val pendingContributions: Unit
        /**
         * Fetches the contributions with the state "IN_PROGRESS", "QUEUED" and "PAUSED" and then it
         * populates the `pendingContributionList`.
         */
        get() {
            val pagedListConfig =
                (PagedList.Config.Builder())
                    .setPrefetchDistance(50)
                    .setPageSize(10).build()
            val factory = repository!!.fetchContributionsWithStates(
                Arrays.asList(
                    Contribution.STATE_IN_PROGRESS, Contribution.STATE_QUEUED,
                    Contribution.STATE_PAUSED
                )
            )

            val livePagedListBuilder = LivePagedListBuilder(factory, pagedListConfig)
            pendingContributionList = livePagedListBuilder.build()
        }

    val failedContributions: Unit
        /**
         * Fetches the contributions with the state "FAILED" and populates the
         * `failedContributionList`.
         */
        get() {
            val pagedListConfig =
                (PagedList.Config.Builder())
                    .setPrefetchDistance(50)
                    .setPageSize(10).build()
            val factory = repository!!.fetchContributionsWithStates(
                listOf(Contribution.STATE_FAILED)
            )

            val livePagedListBuilder = LivePagedListBuilder(factory, pagedListConfig)
            failedContributionList = livePagedListBuilder.build()
        }

    /**
     * Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
     * Fetches the contributions with the state "IN_PROGRESS", "QUEUED", "PAUSED" and "FAILED" and
     * then it populates the `failedAndPendingContributionList`.
     */
    //    void getFailedAndPendingContributions() {
    //        final PagedList.Config pagedListConfig =
    //            (new PagedList.Config.Builder())
    //                .setPrefetchDistance(50)
    //                .setPageSize(10).build();
    //        Factory<Integer, Contribution> factory;
    //        factory = repository.fetchContributionsWithStates(
    //            Arrays.asList(Contribution.STATE_IN_PROGRESS, Contribution.STATE_QUEUED,
    //                Contribution.STATE_PAUSED, Contribution.STATE_FAILED));
    //
    //        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory,
    //            pagedListConfig);
    //        failedAndPendingContributionList = livePagedListBuilder.build();
    //    }

    companion object {
        const val ACTION_INTERNAL_UPLOADS: String = "internalImageUploads"
    }
}
