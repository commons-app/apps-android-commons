/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nguyenhoanglam.imagepicker.R
import com.nguyenhoanglam.imagepicker.helper.ImageHelper
import com.nguyenhoanglam.imagepicker.helper.ToastHelper
import com.nguyenhoanglam.imagepicker.listener.OnImageSelectListener
import com.nguyenhoanglam.imagepicker.model.Config
import com.nguyenhoanglam.imagepicker.model.Image
import com.nguyenhoanglam.imagepicker.ui.imagepicker.GlideLoader

class ImagePickerAdapter(
    context: Context,
    private val config: Config,
    private val imageSelectListener: OnImageSelectListener
) : BaseRecyclerViewAdapter<ImagePickerAdapter.ImageViewHolder?>(context) {

    private val glideLoader = GlideLoader()
    private val selectedImages = arrayListOf<Image>()
    private val disabledImages = arrayListOf<Image>()
    private val images: ArrayList<Image> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val itemView = inflater.inflate(R.layout.imagepicker_item_image, parent, false)
        return ImageViewHolder(
            itemView,
            config.isShowNumberIndicator,
            config.getIndicatorColor(),
            config.getDisabledText()
        )
    }

    override fun onBindViewHolder(
        viewHolder: ImageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(viewHolder, position)
        } else {
            when {
                payloads.any { it is ImageSelectedOrUpdated } -> {
                    if (config.isShowNumberIndicator) {
                        val image = images[position]
                        val selectedIndex = ImageHelper.findImageIndex(image, selectedImages)
                        viewHolder.selectedNumber.text = (selectedIndex + 1).toString()
                        viewHolder.selectedNumber.visibility = View.VISIBLE
                        viewHolder.selectedIcon.visibility = View.GONE
                    } else {
                        viewHolder.selectedIcon.visibility = View.VISIBLE
                        viewHolder.selectedNumber.visibility = View.GONE
                    }
                    setupItemForeground(viewHolder.image, true)
                }
                payloads.any { it is ImageUnselected } -> {
                    if (config.isShowNumberIndicator) viewHolder.selectedNumber.visibility =
                        View.GONE
                    else viewHolder.selectedIcon.visibility = View.GONE
                    setupItemForeground(viewHolder.image, false)
                }
                else -> {
                    onBindViewHolder(viewHolder, position)
                }
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ImageViewHolder, position: Int) {
        val image = images[position]
        val selectedIndex = ImageHelper.findImageIndex(image, selectedImages)
        val disabledIndex = ImageHelper.findImageIndex(image, disabledImages)
        val isSelected = config.isMultipleMode && selectedIndex != -1
        val isDisabled = disabledIndex != -1

        glideLoader.loadImage(image.id, image.path, viewHolder.image)
        setupItemForeground(viewHolder.image, isSelected)
        setupItemForegroundForDisabled(viewHolder.image, isDisabled)

        viewHolder.gifIndicator.visibility =
            if (ImageHelper.isGifFormat(image)) View.VISIBLE else View.GONE
        viewHolder.selectedIcon.visibility =
            if (isSelected && !config.isShowNumberIndicator) View.VISIBLE else View.GONE
        viewHolder.selectedNumber.visibility =
            if (isSelected && config.isShowNumberIndicator) View.VISIBLE else View.GONE
        if (viewHolder.selectedNumber.visibility == View.VISIBLE) {
            viewHolder.selectedNumber.text = (selectedIndex + 1).toString()
        }
        viewHolder.itemView.setOnClickListener {
            if (!isDisabled)
                selectOrRemoveImage(image, position)
            else showToastForDisabled(viewHolder.disabledText)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    private fun setupItemForeground(view: View, isSelected: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.foreground = if (isSelected) ColorDrawable(
                ContextCompat.getColor(
                    context,
                    R.color.imagepicker_black_alpha_30
                )
            ) else null
        }
    }

    private fun setupItemForegroundForDisabled(view: View, isSelected: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.foreground = if (isSelected) ColorDrawable(
                ContextCompat.getColor(
                    context,
                    R.color.imagepicker_color_for_diabled_images
                )
            ) else null
        }
    }

    private fun showToastForDisabled(disabledText: String) {
        Toast.makeText(context, disabledText, Toast.LENGTH_SHORT).show()
    }

    private fun selectOrRemoveImage(image: Image, position: Int) {
        if (config.isMultipleMode) {
            val selectedIndex = ImageHelper.findImageIndex(image, selectedImages)
            if (selectedIndex != -1) {
                selectedImages.removeAt(selectedIndex)
                notifyItemChanged(position, ImageUnselected())
                val indexes = ImageHelper.findImageIndexes(selectedImages, images)
                for (index in indexes) {
                    notifyItemChanged(index, ImageSelectedOrUpdated())
                }
            } else {
                if (selectedImages.size >= config.maxSize) {
                    val message =
                        if (config.limitMessage != null) config.limitMessage!! else String.format(
                            context.resources.getString(R.string.imagepicker_msg_limit_images),
                            config.maxSize
                        )
                    ToastHelper.show(context, message)
                    return
                } else {
                    selectedImages.add(image)
                    notifyItemChanged(position, ImageSelectedOrUpdated())
                }
            }
            imageSelectListener.onSelectedImagesChanged(selectedImages)
        } else {
            imageSelectListener.onSingleModeImageSelected(image)
        }
    }

    fun setData(images: List<Image>) {
        this.images.clear()
        this.images.addAll(images)
        notifyDataSetChanged()
    }

    fun setDisabledImages(disabledImages: ArrayList<Image>) {
        this.disabledImages.clear()
        this.disabledImages.addAll(disabledImages)
        notifyDataSetChanged()
    }

    fun setSelectedImages(selectedImages: ArrayList<Image>) {
        this.selectedImages.clear()
        this.selectedImages.addAll(selectedImages)
        notifyDataSetChanged()

    }

    class ImageViewHolder(
        itemView: View,
        isShowNumberIndicator: Boolean,
        indicatorColor: Int,
        val disabledText: String
    ) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image_thumbnail)
        val selectedIcon: ImageView = itemView.findViewById(R.id.image_selected_icon)
        val selectedNumber: TextView = itemView.findViewById(R.id.text_selected_number)
        val gifIndicator: View = itemView.findViewById(R.id.gif_indicator)

        init {
            val drawable: GradientDrawable =
                (if (isShowNumberIndicator) selectedNumber.background.mutate() else selectedIcon.background.mutate()) as GradientDrawable
            drawable.setColor(indicatorColor)
        }
    }

    class ImageSelectedOrUpdated

    class ImageUnselected
}