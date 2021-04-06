/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.imagepicker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.nguyenhoanglam.imagepicker.listener.OnImageLoaderListener
import com.nguyenhoanglam.imagepicker.model.CallbackStatus
import com.nguyenhoanglam.imagepicker.model.Config
import com.nguyenhoanglam.imagepicker.model.Image
import com.nguyenhoanglam.imagepicker.model.Result

class ImagePickerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val imageFileLoader: ImageFileLoader = ImageFileLoader(context)
    private lateinit var config: Config

    lateinit var selectedImages: MutableLiveData<ArrayList<Image>>
    val result = MutableLiveData(Result(CallbackStatus.IDLE, arrayListOf()))

    lateinit var disabledImages: MutableLiveData<ArrayList<Image>>

    fun setConfig(config: Config) {
        this.config = config
        selectedImages = MutableLiveData(config.selectedImages)
        disabledImages = MutableLiveData(config.disabledImages)
    }

    fun getConfig() = config

    fun fetchImages() {
        result.postValue(Result(CallbackStatus.FETCHING, arrayListOf()))
        imageFileLoader.abortLoadImages()
        imageFileLoader.loadDeviceImages(object : OnImageLoaderListener {
            override fun onImageLoaded(images: ArrayList<Image>) {
                result.postValue(Result(CallbackStatus.SUCCESS, images))
            }

            override fun onFailed(throwable: Throwable) {
                result.postValue(Result(CallbackStatus.SUCCESS, arrayListOf()))
            }

        })
    }
}