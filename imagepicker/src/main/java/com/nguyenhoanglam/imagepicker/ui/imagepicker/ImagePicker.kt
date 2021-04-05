/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.imagepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import com.nguyenhoanglam.imagepicker.R
import com.nguyenhoanglam.imagepicker.model.Config
import com.nguyenhoanglam.imagepicker.model.Image
import com.nguyenhoanglam.imagepicker.ui.camera.CameraActivity
import java.util.*
import kotlin.collections.ArrayList

class ImagePicker(builder: Builder) {

    private var config: Config

    internal class ActivityBuilder(private val activity: Activity) : Builder(activity) {
        override fun start() {
            val intent = intent
            val requestCode = if (config.requestCode != Config.RC_PICK_IMAGES) config.requestCode else Config.RC_PICK_IMAGES
            if (!config.isCameraOnly) {
                activity.startActivityForResult(intent, requestCode)
            } else {
                activity.overridePendingTransition(0, 0)
                activity.startActivityForResult(intent, requestCode)
            }
        }

        override val intent: Intent
            get() {
                val intent: Intent
                if (!config.isCameraOnly) {
                    intent = Intent(activity, ImagePickerActivity::class.java)
                    intent.putExtra(Config.EXTRA_CONFIG, config)
                } else {
                    intent = Intent(activity, CameraActivity::class.java)
                    intent.putExtra(Config.EXTRA_CONFIG, config)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                return intent
            }

    }

    internal class FragmentBuilder(private val fragment: Fragment) : Builder(fragment) {
        override fun start() {
            val intent = intent
            val requestCode = if (config.requestCode != Config.RC_PICK_IMAGES) config.requestCode else Config.RC_PICK_IMAGES
            if (!config.isCameraOnly) {
                fragment.startActivityForResult(intent, requestCode)
            } else {
                fragment.activity?.overridePendingTransition(0, 0)
                fragment.startActivityForResult(intent, requestCode)
            }
        }

        override val intent: Intent
            get() {
                val intent: Intent
                if (!config.isCameraOnly) {
                    intent = Intent(fragment.activity, ImagePickerActivity::class.java)
                    intent.putExtra(Config.EXTRA_CONFIG, config)
                } else {
                    intent = Intent(fragment.activity, CameraActivity::class.java)
                    intent.putExtra(Config.EXTRA_CONFIG, config)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                return intent
            }

    }

    abstract class Builder : BaseBuilder {

        abstract fun start()
        abstract val intent: Intent

        constructor(activity: Activity?) : super(activity)

        constructor(fragment: Fragment) : super(fragment.context)

        fun setToolbarColor(toolbarColor: String): Builder {
            config.setToolbarColor(toolbarColor)
            return this
        }

        fun setStatusBarColor(statusBarColor: String): Builder {
            config.setStatusBarColor(statusBarColor)
            return this
        }

        fun setToolbarTextColor(toolbarTextColor: String): Builder {
            config.setToolbarTextColor(toolbarTextColor)
            return this
        }

        fun setToolbarIconColor(toolbarIconColor: String): Builder {
            config.setToolbarIconColor(toolbarIconColor)
            return this
        }

        fun setProgressBarColor(progressBarColor: String): Builder {
            config.setProgressBarColor(progressBarColor)
            return this
        }


        fun setBackgroundColor(backgroundColor: String): Builder {
            config.setBackgroundColor(backgroundColor)
            return this
        }

        fun setIndicatorColor(indicatorColor: String): Builder {
            config.setIndicatorColor(indicatorColor)
            return this
        }

        fun setCameraOnly(isCameraOnly: Boolean): Builder {
            config.isCameraOnly = isCameraOnly
            return this
        }

        fun setMultipleMode(isMultipleMode: Boolean): Builder {
            config.isMultipleMode = isMultipleMode
            return this
        }

        fun setFolderMode(isFolderMode: Boolean): Builder {
            config.isFolderMode = isFolderMode
            return this
        }

        fun setShowNumberIndicator(isShowNumberIndicator: Boolean): Builder {
            config.isShowNumberIndicator = isShowNumberIndicator
            return this
        }

        fun setShowCamera(isShowCamera: Boolean): Builder {
            config.isShowCamera = isShowCamera
            return this
        }

        fun setMaxSize(maxSize: Int): Builder {
            config.maxSize = maxSize
            return this
        }

        fun setDoneTitle(doneTitle: String): Builder {
            config.doneTitle = doneTitle
            return this
        }

        fun setFolderTitle(folderTitle: String): Builder {
            config.folderTitle = folderTitle
            return this
        }

        fun setImageTitle(imageTitle: String): Builder {
            config.imageTitle = imageTitle
            return this
        }

        fun setLimitMessage(message: String): Builder {
            config.limitMessage = message
            return this
        }

        fun setRootDirectoryName(rootDirectoryName: String): Builder {
            config.rootDirectoryName = rootDirectoryName
            return this
        }

        fun setDirectoryName(directoryName: String): Builder {
            config.directoryName = directoryName
            return this
        }

        fun setAlwaysShowDoneButton(isAlwaysShowDoneButton: Boolean): Builder {
            config.isAlwaysShowDoneButton = isAlwaysShowDoneButton
            return this
        }

        fun setSelectedImages(selectedImages: ArrayList<Image>?): Builder {
            config.selectedImages = selectedImages ?: arrayListOf()
            return this
        }

        fun setDisabledImages(disabledImage: ArrayList<Image>?):Builder{
            config.disabledImages = disabledImage ?: arrayListOf()
            return this
        }

        fun setDisabledText(disabledText: String):Builder{
            config.setDisabledText(disabledText)
            return this
        }

        fun setRequestCode(requestCode: Int): Builder {
            config.requestCode = requestCode
            return this
        }
    }

    abstract class BaseBuilder(context: Context?) {

        var config: Config = Config()

        private fun getDefaultDirectoryName(context: Context): String {
            val pm = context.packageManager
            val ai: ApplicationInfo?
            ai = try {
                pm.getApplicationInfo(context.applicationContext.packageName ?: "", 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            return (if (ai != null) pm.getApplicationLabel(ai) else "Camera") as String
        }

        init {
            val resources = context!!.resources
            config.setToolbarColor("#212121")
            config.setStatusBarColor("#000000")
            config.setToolbarTextColor("#FFFFFF")
            config.setToolbarIconColor("#FFFFFF")
            config.setProgressBarColor("#4CAF50")
            config.setBackgroundColor("#424242")
            config.setIndicatorColor("#1976D2")
            config.isCameraOnly = false
            config.isMultipleMode = true
            config.isFolderMode = true
            config.isShowNumberIndicator = false
            config.isShowCamera = true
            config.maxSize = Config.MAX_SIZE
            config.doneTitle = resources.getString(R.string.imagepicker_action_done)
            config.folderTitle = resources.getString(R.string.imagepicker_title_folder)
            config.imageTitle = resources.getString(R.string.imagepicker_title_image)
            config.rootDirectoryName = Config.ROOT_DIR_DCIM
            config.directoryName = getDefaultDirectoryName(context)
            config.isAlwaysShowDoneButton = false
            config.selectedImages = arrayListOf()
            config.disabledImages = arrayListOf()
            config.setDisabledText("This Image cannot be selected")
        }
    }

    companion object {
        @JvmStatic
        fun with(activity: Activity): Builder {
            return ActivityBuilder(activity)
        }

        @JvmStatic
        fun with(fragment: Fragment): Builder {
            return FragmentBuilder(fragment)
        }

        @JvmStatic
        fun shouldHandleResult(requestCode: Int, resultCode: Int, data: Intent?, callerRequestCode: Int = Config.RC_PICK_IMAGES): Boolean {
            return requestCode == callerRequestCode && resultCode == Activity.RESULT_OK && data != null
        }

        @JvmStatic
        fun getImages(data: Intent?): ArrayList<Image> {
            return if (data != null) data.getParcelableArrayListExtra(Config.EXTRA_IMAGES)
            else arrayListOf()

        }
    }

    init {
        config = builder.config
    }
}