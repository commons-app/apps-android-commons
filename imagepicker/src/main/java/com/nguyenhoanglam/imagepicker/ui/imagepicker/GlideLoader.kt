/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.imagepicker

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.nguyenhoanglam.imagepicker.R


class GlideLoader {

    private val options: RequestOptions = RequestOptions().placeholder(R.drawable.imagepicker_image_placeholder)
        .error(R.drawable.imagepicker_image_placeholder)
        .centerCrop()

    fun loadImage(id: Long, path: String?, imageView: ImageView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            Glide.with(imageView.context)
                .load(uri)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        } else {
            Glide.with(imageView.context)
                .load(path)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }
    }

}