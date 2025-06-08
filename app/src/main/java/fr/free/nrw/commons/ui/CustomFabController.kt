package fr.free.nrw.commons.ui

import android.view.animation.Animation
import android.view.animation.AnimationUtils
import fr.free.nrw.commons.R
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.filepicker.FilePicker
import android.os.Build
import android.Manifest.permission
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import fr.free.nrw.commons.utils.ViewUtil.showShortToast


class CustomFabController(
    private val fragment: Fragment,
    context: Context,
    private val fabPlus: FloatingActionButton,
    private val fabCamera: FloatingActionButton,
    private val fabGallery: FloatingActionButton,
    private val fabCustomGallery: FloatingActionButton,
    private val controller: ContributionController
) {
    private var isFabOpen = false
    private val fabOpen: Animation = AnimationUtils.loadAnimation(context, R.anim.fab_open)
    private val fabClose: Animation = AnimationUtils.loadAnimation(context, R.anim.fab_close)
    private val rotateForward: Animation = AnimationUtils.loadAnimation(context, R.anim.rotate_forward)
    private val rotateBackward: Animation = AnimationUtils.loadAnimation(context, R.anim.rotate_backward)
    private lateinit var inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var galleryPickLauncherForResult: ActivityResultLauncher<Intent>
    private lateinit var customSelectorLauncherForResult: ActivityResultLauncher<Intent>
    private lateinit var cameraPickLauncherForResult: ActivityResultLauncher<Intent>

    fun initializeLaunchers() {
        inAppCameraLocationPermissionLauncher =
            fragment.registerForActivityResult(RequestMultiplePermissions()) { result ->
                val areAllGranted = result.values.all { it }

                if (areAllGranted) {
                    controller.locationPermissionCallback?.onLocationPermissionGranted()
                } else {
                    val activity = fragment.activity
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        activity?.shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION) == true
                    ) {
                        controller.handleShowRationaleFlowCameraLocation(
                            activity,
                            inAppCameraLocationPermissionLauncher,
                            cameraPickLauncherForResult
                        )
                    } else {
                        activity?.getString(R.string.in_app_camera_location_permission_denied)?.let {
                            controller.locationPermissionCallback?.onLocationPermissionDenied(
                                it
                            )
                        }
                    }
                }
            }
        galleryPickLauncherForResult = fragment.registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
            controller.handleActivityResultWithCallback(fragment.requireActivity()) { callbacks: FilePicker.Callbacks? ->
                controller.onPictureReturnedFromGallery(result!!, fragment.requireActivity(), callbacks!!)
            }
        }

        customSelectorLauncherForResult = fragment.registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
            controller.handleActivityResultWithCallback(fragment.requireActivity()) { callbacks: FilePicker.Callbacks? ->
                controller.onPictureReturnedFromCustomSelector(result!!, fragment.requireActivity(), callbacks!!)
            }
        }

        cameraPickLauncherForResult = fragment.registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
            controller.handleActivityResultWithCallback(fragment.requireActivity()) { callbacks: FilePicker.Callbacks? ->
                controller.onPictureReturnedFromCamera(result!!, fragment.requireActivity(), callbacks!!)
            }
        }
    }


    private fun toggleFabMenu() {
        isFabOpen = !isFabOpen
        if (fabPlus.isShown) {
            if (isFabOpen) {
                fabPlus.startAnimation(rotateForward)
                fabCamera.startAnimation(fabOpen)
                fabGallery.startAnimation(fabOpen)
                fabCustomGallery.startAnimation(fabOpen)

                fabCamera.show()
                fabGallery.show()
                fabCustomGallery.show()
            } else {
                fabPlus.startAnimation(rotateBackward)
                fabCamera.startAnimation(fabClose)
                fabGallery.startAnimation(fabClose)
                fabCustomGallery.startAnimation(fabClose)

                fabCamera.hide()
                fabGallery.hide()
                fabCustomGallery.hide()
            }
        }
    }

    private fun closeFabMenu() {
        if (isFabOpen) toggleFabMenu()
    }

    fun closeFabMenuIfOpen() {
        if (isFabOpen) {
            closeFabMenu()
        }
    }

    fun setListeners(controller: ContributionController, activity: Activity) {
        fabPlus.setOnClickListener {
            toggleFabMenu()
        }

        fabCamera.setOnClickListener {
            controller.initiateCameraPick(
                activity,
                inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult
            )
            closeFabMenu()
        }

        fabCamera.setOnLongClickListener {
            showShortToast(activity, R.string.add_contribution_from_camera)
            true
        }

        fabGallery.setOnClickListener {
            controller.initiateGalleryPick(activity, galleryPickLauncherForResult, true)
            closeFabMenu()
        }

        fabGallery.setOnLongClickListener {
            showShortToast(activity, R.string.menu_from_gallery)
            true
        }

        fabCustomGallery.setOnClickListener {
            controller.initiateCustomGalleryPickWithPermission(
                activity,
                customSelectorLauncherForResult
            )
            closeFabMenu()
        }

        fabCustomGallery.setOnLongClickListener {
            showShortToast(activity, R.string.custom_selector_title)
            true
        }
    }



}