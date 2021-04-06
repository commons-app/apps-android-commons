/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.helper

import android.util.Log

class LogHelper private constructor() {

    private var isEnable = true

    fun setEnable(enable: Boolean) {
        isEnable = enable
    }

    fun d(message: String) {
        if (isEnable) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String) {
        if (isEnable) {
            Log.e(TAG, message)
        }
    }

    fun w(message: String) {
        if (isEnable) {
            Log.w(TAG, message)
        }
    }

    companion object {
        private const val TAG = "ImagePicker"
        private var INSTANCE: LogHelper? = null

        @JvmStatic
        val instance: LogHelper?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = LogHelper()
                }
                return INSTANCE
            }
    }
}