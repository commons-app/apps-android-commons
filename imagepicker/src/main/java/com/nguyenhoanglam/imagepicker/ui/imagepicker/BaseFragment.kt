/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.imagepicker

import android.content.res.Configuration
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOnConfigurationChanged()
    }

    abstract fun handleOnConfigurationChanged()
}