package fr.free.nrw.commons.contributions

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.FilePicker.openCameraForImage
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.PermissionUtils.PERMISSIONS_STORAGE
import fr.free.nrw.commons.utils.PermissionUtils.checkPermissionsAndPerformActionWithPermissions
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Owns camera launch, location permission coordination, and camera-specific upload state.
 */
@Singleton
class ContributionCameraCoordinator
    @Inject
    constructor(
        @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
    ) {
        private var locationBeforeImageCapture: LatLng? = null
        private var isInAppCameraUpload = false
        private var locationPermissionCallback: LocationPermissionCallback? = null
        private var locationPermissionsHelper: LocationPermissionsHelper? = null

        @JvmField
        @Inject
        var locationManager: LocationServiceManager? = null

        /**
         * Begins the camera flow, requesting storage access when configured and optionally location access.
         *
         * @param activity activity launching the camera flow
         * @param inAppCameraLocationPermissionLauncher launcher for location permissions
         * @param resultLauncher launcher for the camera activity
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
         * Dispatches a location permission result through the camera permission flow.
         *
         * @param activity activity requesting location permission
         * @param results outcomes from the permission launcher
         * @param inAppCameraLocationPermissionLauncher launcher reused by the rationale
         * @param resultLauncher launcher for the camera activity
         */
        fun handleLocationPermissionResult(
            activity: Activity?,
            results: Map<String, Boolean>,
            inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            if (results.values.all { it }) {
                locationPermissionCallback?.onLocationPermissionGranted()
            } else if (
                activity != null &&
                activity.shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)
            ) {
                handleShowRationaleFlowCameraLocation(
                    activity,
                    inAppCameraLocationPermissionLauncher,
                    resultLauncher,
                )
            } else if (activity != null) {
                locationPermissionCallback?.onLocationPermissionDenied(
                    activity.getString(R.string.in_app_camera_location_permission_denied),
                )
            }
        }

        /**
         * Shows a rationale before requesting camera location access again.
         *
         * @param activity activity displaying the rationale
         * @param inAppCameraLocationPermissionLauncher launcher for location permissions
         * @param resultLauncher launcher for the camera activity
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
         * Returns camera metadata for upload and resets the camera-upload flag.
         */
        internal fun takeUploadMetadata(): CameraUploadMetadata {
            val metadata = CameraUploadMetadata(locationBeforeImageCapture, isInAppCameraUpload)
            isInAppCameraUpload = false
            return metadata
        }

        /**
         * Creates location callbacks and requests permission before camera capture.
         *
         * @param activity activity requesting location access
         * @param inAppCameraLocationPermissionLauncher launcher for location permissions
         * @param resultLauncher launcher for the camera activity
         */
        private fun createDialogsAndHandleLocationPermissions(
            activity: Activity,
            inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>?,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            locationPermissionCallback =
                object : LocationPermissionCallback {
                    /**
                     * Shows the denial message and continues camera upload.
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
                     * Checks device location availability before continuing camera upload.
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
         * Asks whether camera uploads should request location access.
         *
         * @param activity activity displaying the choice
         * @param inAppCameraLocationPermissionLauncher launcher for location permissions
         * @param resultLauncher launcher for the camera activity
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
         * Configures file-picker settings and launches the camera.
         *
         * @param activity activity launching the camera
         * @param resultLauncher launcher for the camera activity
         */
        private fun initiateCameraUpload(
            activity: Activity,
            resultLauncher: ActivityResultLauncher<Intent>,
        ) {
            configureContributionFilePicker(activity, defaultKvStore, false)
            if (defaultKvStore.getBoolean("inAppCameraLocationPref", false)) {
                locationBeforeImageCapture = locationManager!!.getLastLocation()
            }
            isInAppCameraUpload = true
            openCameraForImage(activity, resultLauncher, 0)
        }
    }

/**
 * Camera-specific metadata attached to an upload handoff.
 *
 * @property locationBeforeImageCapture last known location captured before camera launch
 * @property isInAppCameraUpload whether the upload originated from the in-app camera flow
 */
internal data class CameraUploadMetadata(
    val locationBeforeImageCapture: LatLng?,
    val isInAppCameraUpload: Boolean,
)
