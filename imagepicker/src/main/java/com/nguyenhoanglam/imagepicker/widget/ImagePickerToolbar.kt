/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.nguyenhoanglam.imagepicker.R
import com.nguyenhoanglam.imagepicker.model.Config

class ImagePickerToolbar : RelativeLayout {

    private lateinit var titleText: TextView
    private lateinit var doneText: TextView
    private lateinit var backImage: AppCompatImageView
    private lateinit var cameraImage: AppCompatImageView

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.imagepicker_toolbar, this)
        if (isInEditMode) {
            return
        }
        titleText = findViewById(R.id.text_toolbar_title)
        doneText = findViewById(R.id.text_toolbar_done)
        backImage = findViewById(R.id.image_toolbar_back)
        cameraImage = findViewById(R.id.image_toolbar_camera)
    }

    fun config(config: Config) {
        setBackgroundColor(config.getToolbarColor())
        titleText.text = if (config.isFolderMode) config.folderTitle else config.imageTitle
        titleText.setTextColor(config.getToolbarTextColor())
        doneText.text = config.doneTitle
        doneText.setTextColor(config.getToolbarTextColor())
        doneText.visibility = if (config.isAlwaysShowDoneButton) View.VISIBLE else View.GONE
        backImage.setColorFilter(config.getToolbarIconColor())
        cameraImage.setColorFilter(config.getToolbarIconColor())
        cameraImage.visibility = if (config.isShowCamera) View.VISIBLE else View.GONE
    }

    fun setTitle(title: String?) {
        titleText.text = title
    }

    fun showDoneButton(isShow: Boolean) {
        doneText.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setOnBackClickListener(clickListener: OnClickListener) {
        backImage.setOnClickListener(clickListener)
    }

    fun setOnCameraClickListener(clickListener: OnClickListener) {
        cameraImage.setOnClickListener(clickListener)
    }

    fun setOnDoneClickListener(clickListener: OnClickListener) {
        doneText.setOnClickListener(clickListener)
    }
}