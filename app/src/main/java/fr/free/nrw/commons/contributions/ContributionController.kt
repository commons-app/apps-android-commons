package fr.free.nrw.commons.contributions

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import fr.free.nrw.commons.filepicker.ExternalImagesResult
import fr.free.nrw.commons.filepicker.FilePicker.handleExternalImagesPicked
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContributionController
    @Inject
    constructor(
        @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
    ) {
        private var locationBeforeImageCapture: LatLng? = null
        private var isInAppCameraUpload = false

        // Flag to track if an internal upload is in progress.
        @Volatile
        var isInternalUploadInProgress = false

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
        var repository: ContributionsRepository? = null

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

            checkPermissionsAndPerformAction(
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
                *PERMISSIONS_STORAGE,
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
                    override fun onLocationPermissionDenied(toastMessage: String) {
                        Toast
                            .makeText(
                                activity,
                                toastMessage,
                                Toast.LENGTH_LONG,
                            ).show()
                        initiateCameraUpload(activity, resultLauncher)
                    }

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
            initiateGalleryUpload(activity, resultLauncher, allowMultipleUploads)
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

            checkPermissionsAndPerformAction(
                activity,
                { FilePicker.openCustomSelector(activity, resultLauncher, 0, singleSelection) },
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale,
                *PERMISSIONS_STORAGE,
            )
        }

        /**
         * Open chooser for gallery uploads
         */
        private fun initiateGalleryUpload(
            activity: Activity,
            resultLauncher: ActivityResultLauncher<Intent>,
            allowMultipleUploads: Boolean,
        ) {
            setPickerConfiguration(activity, allowMultipleUploads)
            openGallery(activity, resultLauncher, 0, isDocumentPhotoPickerPreferred)
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

        private val isDocumentPhotoPickerPreferred: Boolean
            get() =
                defaultKvStore.getBoolean(
                    "openDocumentPhotoPickerPref",
                    true,
                )

        fun onPictureReturnedFromGallery(
            result: ActivityResult,
            activity: Activity,
            callbacks: FilePicker.Callbacks,
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
            callbacks: FilePicker.Callbacks,
        ) {
            FilePicker.onPictureReturnedFromCustomSelector(result, activity, callbacks)
        }

        fun onPictureReturnedFromCamera(
            result: ActivityResult,
            activity: Activity,
            callbacks: FilePicker.Callbacks,
        ) {
            FilePicker.onPictureReturnedFromCamera(result, activity, callbacks)
        }

        /**
         * Attaches callback for file picker.
         */
        fun handleActivityResultWithCallback(
            activity: Activity,
            handleActivityResult: HandleActivityResult,
        ) {
            handleActivityResult.onHandleActivityResult(
                object : DefaultCallback() {
                    override fun onCanceled(
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        super.onCanceled(source, type)
                        isInternalUploadInProgress = false // reset flag on cancel
                        defaultKvStore.remove(PLACE_OBJECT)
                    }

                    override fun onImagePickerError(
                        e: Exception,
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        isInternalUploadInProgress = false
                        showShortToast(activity, R.string.error_occurred_in_picking_images)
                    }

                    override fun onImagesPicked(
                        imagesFiles: List<UploadableFile>,
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        val intent = handleImagesPicked(activity, imagesFiles)
                        isInternalUploadInProgress = false
                        activity.startActivity(intent)
                    }
                },
            )
        }

        fun handleExternalImagesPicked(
            activity: Activity,
            data: Intent?,
        ): ExternalImagesResult = handleExternalImagesPicked(data, activity)

        val pendingContributions: Unit
            /**
             * Fetches the contributions with the state "IN_PROGRESS", "QUEUED" and "PAUSED" and then it
             * populates the `pendingContributionList`.
             */
            get() {
                val pagedListConfig =
                    (PagedList.Config.Builder())
                        .setPrefetchDistance(50)
                        .setPageSize(10)
                        .build()
                val factory =
                    repository!!.fetchContributionsWithStates(
                        Arrays.asList(
                            Contribution.STATE_IN_PROGRESS,
                            Contribution.STATE_QUEUED,
                            Contribution.STATE_PAUSED,
                            Contribution.STATE_METADATA_PENDING,
                        ),
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
                        .setPageSize(10)
                        .build()
                val factory =
                    repository!!.fetchContributionsWithStates(
                        listOf(Contribution.STATE_FAILED),
                    )

                val livePagedListBuilder = LivePagedListBuilder(factory, pagedListConfig)
                failedContributionList = livePagedListBuilder.build()
            }

        // Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
        // Fetches the contributions with the state "IN_PROGRESS", "QUEUED", "PAUSED" and "FAILED" and
        // then it populates the `failedAndPendingContributionList`.
        //
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
