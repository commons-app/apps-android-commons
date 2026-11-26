package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.View
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.ImageMetadata
import fr.free.nrw.commons.customselector.helper.ImageMetadataHelper
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.ImageViewMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListItemDetailsBinder(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun bind(
        holder: ImageAdapter.ImageViewHolder,
        position: Int,
        image: Image,
        viewMode: ImageViewMode,
    ) {
        if (viewMode != ImageViewMode.LIST) return

        holder.imageName?.text = image.name
        holder.imageDetails?.text = null
        holder.locationIndicator?.visibility = View.GONE
        scope.launch(ioDispatcher) {
            val metadata = ImageMetadataHelper.getOrLoad(image)
            val details = buildDetails(metadata)
            withContext(Dispatchers.Main) {
                if (holder.bindingAdapterPosition == position && viewMode == ImageViewMode.LIST) {
                    holder.imageName?.text = image.name
                    holder.imageDetails?.text = details
                    holder.locationIndicator?.visibility =
                        if (metadata.hasGeolocation) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun buildDetails(metadata: ImageMetadata): String =
        buildList {
            if (metadata.width > 0 && metadata.height > 0) {
                add(
                    context.getString(
                        R.string.custom_selector_image_dimensions,
                        metadata.width,
                        metadata.height,
                    ),
                )
            }
            if (metadata.sizeBytes > 0) {
                add(Formatter.formatFileSize(context, metadata.sizeBytes))
            }
            add(
                if (metadata.hasGeolocation) {
                    context.getString(R.string.custom_selector_image_has_location)
                } else {
                    context.getString(R.string.custom_selector_image_no_location)
                },
            )
        }.joinToString(" · ")
}
