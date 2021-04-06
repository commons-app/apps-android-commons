/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nguyenhoanglam.imagepicker.R
import com.nguyenhoanglam.imagepicker.helper.CameraHelper.checkCameraAvailability
import com.nguyenhoanglam.imagepicker.helper.LogHelper.Companion.instance
import com.nguyenhoanglam.imagepicker.helper.PermissionHelper.hasGranted
import com.nguyenhoanglam.imagepicker.helper.PermissionHelper.hasSelfPermission
import com.nguyenhoanglam.imagepicker.helper.PermissionHelper.hasSelfPermissions
import com.nguyenhoanglam.imagepicker.helper.PermissionHelper.openAppSettings
import com.nguyenhoanglam.imagepicker.helper.PermissionHelper.requestAllPermissions
import com.nguyenhoanglam.imagepicker.helper.PermissionHelper.shouldShowRequestPermissionRationale
import com.nguyenhoanglam.imagepicker.helper.PreferenceHelper.firstTimeAskingPermission
import com.nguyenhoanglam.imagepicker.helper.PreferenceHelper.isFirstTimeAskingPermission
import com.nguyenhoanglam.imagepicker.helper.ToastHelper
import com.nguyenhoanglam.imagepicker.model.Config
import com.nguyenhoanglam.imagepicker.model.Image
import kotlinx.android.synthetic.main.imagepicker_activity_camera.*
import java.util.*

class CameraActivity : AppCompatActivity() {

    private val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
    private var config: Config? = null
    private val cameraModule: CameraModule = DefaultCameraModule()
    private val logger = instance
    private var isOpeningCamera = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent == null) {
            finish()
            return
        }

        config = intent.getParcelableExtra(Config.EXTRA_CONFIG)
        setContentView(R.layout.imagepicker_activity_camera)
    }

    override fun onResume() {
        super.onResume()
        if (hasSelfPermissions(this, permissions) && isOpeningCamera) {
            isOpeningCamera = false
        } else if (!snackbar.isShowing) {
            captureImageWithPermission()
        }
    }

    private fun captureImageWithPermission() {
        if (hasSelfPermissions(this, permissions)) {
            captureImage()
        } else {
            logger?.w("Camera permission is not granted. Requesting permission")
            requestCameraPermission()
        }
    }

    private fun captureImage() {
        if (!checkCameraAvailability(this)) {
            finish()
            return
        }
        val intent = cameraModule.getCameraIntent(this, config!!)
        if (intent == null) {
            ToastHelper.show(this, getString(R.string.imagepicker_error_create_image_file))
            return
        }
        startActivityForResult(intent, Config.RC_CAPTURE_IMAGE)
        isOpeningCamera = true
    }

    private fun requestCameraPermission() {
        logger?.w("Write External permission is not granted. Requesting permission...")
        var hasPermissionDisabled = false
        val wesGranted = hasSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val cameraGranted = hasSelfPermission(this, Manifest.permission.CAMERA)
        if (!wesGranted && !shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (!isFirstTimeAskingPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                hasPermissionDisabled = true
            }
        }
        if (!cameraGranted && !shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            if (!isFirstTimeAskingPermission(this, Manifest.permission.CAMERA)) {
                hasPermissionDisabled = true
            }
        }
        val permissions: MutableList<String> = ArrayList()
        if (!hasPermissionDisabled) {
            if (!wesGranted) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                firstTimeAskingPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, false)
            }
            if (!cameraGranted) {
                permissions.add(Manifest.permission.CAMERA)
                firstTimeAskingPermission(this, Manifest.permission.CAMERA, false)
            }
            requestAllPermissions(this, permissions.toTypedArray(), Config.RC_CAMERA_PERMISSION)
        } else {
            snackbar.show(R.string.imagepicker_msg_no_write_external_storage_camera_permission, View.OnClickListener {
                openAppSettings(this@CameraActivity)
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Config.RC_CAMERA_PERMISSION -> {
                if (hasGranted(grantResults)) {
                    logger?.d("Camera permission granted")
                    captureImage()
                    return
                }
                logger?.e("Permission not granted: results len = " + grantResults.size + " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)")
                var shouldShowSnackBar = false
                for (grantResult in grantResults) {
                    if (hasGranted(grantResult)) {
                        shouldShowSnackBar = true
                        break
                    }
                }
                if (shouldShowSnackBar) {
                    snackbar.show(R.string.imagepicker_msg_no_write_external_storage_camera_permission, View.OnClickListener {
                        openAppSettings(this@CameraActivity)
                    })
                } else {
                    finish()
                }
            }
            else -> {
                logger?.d("Got unexpected permission result: $requestCode")
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Config.RC_CAPTURE_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                finishCaptureImage()
            } else {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        }
    }

    private fun finishCaptureImage() {
        cameraModule.getImage(this, config!!.isCameraOnly, object : OnImageReadyListener {
            override fun onImageReady(images: ArrayList<Image>) {
                val data = Intent()
                data.putParcelableArrayListExtra(Config.EXTRA_IMAGES, images)
                setResult(Activity.RESULT_OK, data)
                finish()
            }

            override fun onImageNotReady() {
                val data = Intent()
                data.putParcelableArrayListExtra(Config.EXTRA_IMAGES, arrayListOf())
                setResult(Activity.RESULT_OK, data)
                finish()
            }

        })
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

}