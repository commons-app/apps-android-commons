/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.model

import android.graphics.Color
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import java.util.*
import kotlin.collections.ArrayList

class Config() : Parcelable {

    private lateinit var toolbarColor: String
    private lateinit var statusBarColor: String
    private lateinit var toolbarTextColor: String
    private lateinit var toolbarIconColor: String
    private lateinit var progressBarColor: String
    private lateinit var backgroundColor: String
    private lateinit var indicatorColor: String
    private lateinit var disabledText: String
    var isCameraOnly = false
    var isMultipleMode = false
    var isFolderMode = false
    var isShowNumberIndicator = false
    var isShowCamera = false
    var maxSize = MAX_SIZE
    lateinit var doneTitle: String
    lateinit var folderTitle: String
    lateinit var imageTitle: String
    var limitMessage: String? = null
    lateinit var rootDirectoryName: String
    lateinit var directoryName: String
    var isAlwaysShowDoneButton = false
    lateinit var selectedImages: ArrayList<Image>
    lateinit var disabledImages: ArrayList<Image>
    var requestCode = RC_PICK_IMAGES

    constructor(parcel: Parcel) : this() {
        toolbarColor = parcel.readString()!!
        statusBarColor = parcel.readString()!!
        toolbarTextColor = parcel.readString()!!
        toolbarIconColor = parcel.readString()!!
        progressBarColor = parcel.readString()!!
        backgroundColor = parcel.readString()!!
        indicatorColor = parcel.readString()!!
        isCameraOnly = parcel.readByte() != 0.toByte()
        isMultipleMode = parcel.readByte() != 0.toByte()
        isFolderMode = parcel.readByte() != 0.toByte()
        isShowNumberIndicator = parcel.readByte() != 0.toByte()
        isShowCamera = parcel.readByte() != 0.toByte()
        maxSize = parcel.readInt()
        doneTitle = parcel.readString()!!
        folderTitle = parcel.readString()!!
        imageTitle = parcel.readString()!!
        limitMessage = parcel.readString()
        rootDirectoryName = parcel.readString()!!
        directoryName = parcel.readString()!!
        isAlwaysShowDoneButton = parcel.readByte() != 0.toByte()
        selectedImages = parcel.createTypedArrayList(Image.CREATOR)!!
        disabledImages = parcel.createTypedArrayList(Image.CREATOR)!!
        disabledText = parcel.readString()!!
        requestCode = parcel.readInt()
    }


    fun getToolbarColor(): Int {
        return Color.parseColor(toolbarColor)
    }

    fun setToolbarColor(toolbarColor: String) {
        this.toolbarColor = toolbarColor
    }

    fun getStatusBarColor(): Int {
        return Color.parseColor(statusBarColor)
    }

    fun setStatusBarColor(statusBarColor: String) {
        this.statusBarColor = statusBarColor
    }

    fun getToolbarTextColor(): Int {
        return Color.parseColor(toolbarTextColor)
    }

    fun setToolbarTextColor(toolbarTextColor: String) {
        this.toolbarTextColor = toolbarTextColor
    }

    fun getToolbarIconColor(): Int {
        return Color.parseColor(toolbarIconColor)
    }

    fun setToolbarIconColor(toolbarIconColor: String) {
        this.toolbarIconColor = toolbarIconColor
    }

    fun getProgressBarColor(): Int {
        return Color.parseColor(progressBarColor)
    }

    fun setProgressBarColor(progressBarColor: String) {
        this.progressBarColor = progressBarColor
    }

    fun getBackgroundColor(): Int {
        return Color.parseColor(backgroundColor)
    }

    fun setBackgroundColor(backgroundColor: String) {
        this.backgroundColor = backgroundColor
    }

    fun getIndicatorColor(): Int {
        return Color.parseColor(indicatorColor)
    }

    fun setIndicatorColor(indicatorColor: String) {
        this.indicatorColor = indicatorColor
    }

    fun getDisabledText(): String{
        return disabledText
    }

    fun setDisabledText(disabledText: String){
        this.disabledText = disabledText
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(toolbarColor)
        parcel.writeString(statusBarColor)
        parcel.writeString(toolbarTextColor)
        parcel.writeString(toolbarIconColor)
        parcel.writeString(progressBarColor)
        parcel.writeString(backgroundColor)
        parcel.writeString(indicatorColor)
        parcel.writeByte(if (isCameraOnly) 1 else 0)
        parcel.writeByte(if (isMultipleMode) 1 else 0)
        parcel.writeByte(if (isFolderMode) 1 else 0)
        parcel.writeByte(if (isShowNumberIndicator) 1 else 0)
        parcel.writeByte(if (isShowCamera) 1 else 0)
        parcel.writeInt(maxSize)
        parcel.writeString(doneTitle)
        parcel.writeString(folderTitle)
        parcel.writeString(imageTitle)
        parcel.writeString(limitMessage)
        parcel.writeString(rootDirectoryName)
        parcel.writeString(directoryName)
        parcel.writeByte(if (isAlwaysShowDoneButton) 1 else 0)
        parcel.writeTypedList(selectedImages)
        parcel.writeTypedList(disabledImages)
        parcel.writeString(disabledText)
        parcel.writeInt(requestCode)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Config> {

        const val EXTRA_CONFIG = "ImagePickerConfig"
        const val EXTRA_IMAGES = "ImagePickerImages"
        const val RC_PICK_IMAGES = 100
        const val RC_CAPTURE_IMAGE = 101
        const val RC_WRITE_EXTERNAL_STORAGE_PERMISSION = 102
        const val RC_CAMERA_PERMISSION = 103
        const val MAX_SIZE = Int.MAX_VALUE
        val ROOT_DIR_DCIM: String = Environment.DIRECTORY_DCIM
        val ROOT_DIR_DOWNLOAD: String = Environment.DIRECTORY_DOWNLOADS
        val ROOT_DIR_PICTURES: String = Environment.DIRECTORY_PICTURES

        override fun createFromParcel(parcel: Parcel): Config {
            return Config(parcel)
        }

        override fun newArray(size: Int): Array<Config?> {
            return arrayOfNulls(size)
        }
    }
}