/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.listener

import com.nguyenhoanglam.imagepicker.model.Image

interface OnImageLoaderListener {
    fun onImageLoaded(images: ArrayList<Image>)
    fun onFailed(throwable: Throwable)
}