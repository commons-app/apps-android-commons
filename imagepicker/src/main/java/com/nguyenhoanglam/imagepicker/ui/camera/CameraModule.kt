/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.camera

import android.content.Context
import android.content.Intent
import com.nguyenhoanglam.imagepicker.model.Config

interface CameraModule {
    fun getCameraIntent(context: Context, config: Config): Intent?
    fun getImage(context: Context, isRequireId: Boolean, imageReadyListener: OnImageReadyListener?)
}