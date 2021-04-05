/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.listener

import com.nguyenhoanglam.imagepicker.model.Image

interface OnImageSelectListener {
    fun onSelectedImagesChanged(selectedImages: ArrayList<Image>)
    fun onSingleModeImageSelected(image: Image)
}