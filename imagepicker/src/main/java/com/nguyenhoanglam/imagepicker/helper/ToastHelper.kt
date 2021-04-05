/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.helper

import android.content.Context
import android.widget.Toast

class ToastHelper {
    companion object {
        var toast: Toast? = null
        fun show(context: Context, text: String, duration: Int = Toast.LENGTH_SHORT) {
            if (toast == null) {
                toast = Toast.makeText(context.applicationContext, text, duration)
            } else {
                toast?.setText(text)
            }
            toast?.show()
        }
    }
}