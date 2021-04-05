/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.imagepicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.nguyenhoanglam.imagepicker.R
import com.nguyenhoanglam.imagepicker.helper.*
import com.nguyenhoanglam.imagepicker.listener.OnFolderClickListener
import com.nguyenhoanglam.imagepicker.listener.OnImageSelectListener
import com.nguyenhoanglam.imagepicker.model.Config
import com.nguyenhoanglam.imagepicker.model.Folder
import com.nguyenhoanglam.imagepicker.model.Image
import com.nguyenhoanglam.imagepicker.ui.camera.DefaultCameraModule
import com.nguyenhoanglam.imagepicker.ui.camera.OnImageReadyListener
import kotlinx.android.synthetic.main.imagepicker_activity_imagepicker.*
import java.io.File

class ImagePickerActivity : AppCompatActivity(), OnFolderClickListener, OnImageSelectListener {

    private var config: Config? = null
    private lateinit var viewModel: ImagePickerViewModel
    private val cameraModule = DefaultCameraModule()
    private val logger = LogHelper.instance

    private val backClickListener = View.OnClickListener { onBackPressed() }
    private val cameraClickListener = View.OnClickListener { captureImageWithPermission() }
    private val doneClickListener = View.OnClickListener { onDone() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent == null) {
            finish()
            return
        }

        config = intent.getParcelableExtra(Config.EXTRA_CONFIG)
        setContentView(R.layout.imagepicker_activity_imagepicker)

        viewModel = ViewModelProvider(this, ImagePickerViewModelFactory(this.application)).get(ImagePickerViewModel::class.java)
        viewModel.setConfig(config!!)
        viewModel.selectedImages.observe(this, Observer {
            toolbar.showDoneButton(config!!.isAlwaysShowDoneButton || it.isNotEmpty())
        })

        setupViews()
    }

    override fun onResume() {
        super.onResume()
        fetchDataWithPermission()
    }

    private fun setupViews() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = config!!.getStatusBarColor()
        }

        toolbar.config(config!!)
        toolbar.setOnBackClickListener(backClickListener)
        toolbar.setOnCameraClickListener(cameraClickListener)
        toolbar.setOnDoneClickListener(doneClickListener)

        val initialFragment = if (config!!.isFolderMode) FolderFragment.newInstance() else ImageFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, initialFragment)
            .commit()
    }


    private fun fetchDataWithPermission() {
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        PermissionHelper.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, object : PermissionHelper.PermissionAskListener {
            override fun onNeedPermission() {
                PermissionHelper.requestAllPermissions(this@ImagePickerActivity, permissions, Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION)
            }

            override fun onPermissionPreviouslyDenied() {
                PermissionHelper.requestAllPermissions(this@ImagePickerActivity, permissions, Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION)
            }

            override fun onPermissionDisabled() {
                snackbar.show(R.string.imagepicker_msg_no_write_external_storage_permission, View.OnClickListener {
                    PermissionHelper.openAppSettings(this@ImagePickerActivity)
                })
            }

            override fun onPermissionGranted() {
                fetchData()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION -> {
                if (PermissionHelper.hasGranted(grantResults)) {
                    logger?.d("Write External permission granted")
                    fetchData()
                } else {
                    logger?.e("Permission not granted: results len = " + grantResults.size)
                    logger?.e("Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")
                    finish()
                }
            }
            Config.RC_CAMERA_PERMISSION -> {
                if (PermissionHelper.hasGranted(grantResults)) {
                    logger?.d("Camera permission granted")
                    captureImage()

                } else {
                    logger?.e("Permission not granted: results len = " + grantResults.size + " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")
                }
            }
            else -> {
                logger?.d("Got unexpected permission result: $requestCode")
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun fetchData() {
        viewModel.fetchImages()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment != null && fragment is FolderFragment) {
            toolbar.setTitle(config!!.folderTitle)
        }
    }

    private fun onDone() {
        val selectedImages = viewModel.selectedImages.value
        if (selectedImages != null && selectedImages.isNotEmpty()) {
            var i = 0
            while (i < selectedImages.size) {
                val (_, _, _, path) = selectedImages[i]
                val file = File(path)
                if (!file.exists()) {
                    selectedImages.removeAt(i)
                    i--
                }
                i++
            }
            finishPickImages(selectedImages)
        } else {
            finishPickImages(arrayListOf())
        }
    }


    private fun captureImageWithPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        PermissionHelper.checkPermission(this, Manifest.permission.CAMERA, object : PermissionHelper.PermissionAskListener {
            override fun onNeedPermission() {
                PermissionHelper.requestAllPermissions(this@ImagePickerActivity, permissions, Config.RC_CAMERA_PERMISSION)
            }

            override fun onPermissionPreviouslyDenied() {
                PermissionHelper.requestAllPermissions(this@ImagePickerActivity, permissions, Config.RC_CAMERA_PERMISSION)
            }

            override fun onPermissionDisabled() {
                snackbar.show(R.string.imagepicker_msg_no_camera_permission, View.OnClickListener {
                    PermissionHelper.openAppSettings(this@ImagePickerActivity)
                })
            }

            override fun onPermissionGranted() {
                captureImage()
            }
        })
    }


    fun captureImage() {
        if (!CameraHelper.checkCameraAvailability(this)) {
            return
        }

        val intent = cameraModule.getCameraIntent(this, config!!)
        if (intent == null) {
            ToastHelper.show(this, getString(R.string.imagepicker_error_create_image_file))
            return
        }
        startActivityForResult(intent, Config.RC_CAPTURE_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Config.RC_CAPTURE_IMAGE && resultCode == Activity.RESULT_OK) {
            cameraModule.getImage(this, config!!.isCameraOnly, object : OnImageReadyListener {
                override fun onImageReady(images: ArrayList<Image>) {
                    fetchDataWithPermission()
                }

                override fun onImageNotReady() {
                    logger?.e("Could not get captured image's path")
                    fetchDataWithPermission()
                }

            })
        }
    }

    private fun finishPickImages(images: ArrayList<Image>) {
        val data = Intent()
        data.putParcelableArrayListExtra(Config.EXTRA_IMAGES, images)
        setResult(Activity.RESULT_OK, data)
        finish()
    }


    override fun onFolderClick(folder: Folder) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, ImageFragment.newInstance(folder.bucketId))
            .addToBackStack(null)
            .commit()
        toolbar.setTitle(folder.name)
    }

    override fun onSelectedImagesChanged(selectedImages: ArrayList<Image>) {
        viewModel.selectedImages.value = selectedImages
    }

    override fun onSingleModeImageSelected(image: Image) {
        finishPickImages(ImageHelper.singleListFromImage(image))
    }
}