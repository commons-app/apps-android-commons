package fr.free.nrw.commons.upload

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ItemUploadThumbnailBinding
import fr.free.nrw.commons.filepicker.UploadableFile
import java.io.File

/**
 * The adapter class for image thumbnails to be shown while uploading.
 */
private const val BORDER_STROKE_WIDTH_PX = 8
private const val ELEVATION_SELECTED_DP = 10f
private const val ELEVATION_UNSELECTED_DP = 0f
private const val ALPHA_SELECTED = 1.0f
private const val ALPHA_UNSELECTED = 0.7f
internal class ThumbnailsAdapter(
    private val callback: Callback,
) : RecyclerView.Adapter<ThumbnailsAdapter.ViewHolder>() {
    var onThumbnailDeletedListener: OnThumbnailDeletedListener? = null
    var uploadableFiles: List<UploadableFile> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        i: Int,
    ) = ViewHolder(
        ItemUploadThumbnailBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false,
        ),
    )

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        position: Int,
    ) = viewHolder.bind(position)

    override fun getItemCount(): Int = uploadableFiles.size

    inner class ViewHolder(
        val binding: ItemUploadThumbnailBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val rlContainer: RelativeLayout = binding.rlContainer
        private val background: SimpleDraweeView = binding.ivThumbnail
        private val ivError: ImageView = binding.ivError
        private val ivCross: ImageView = binding.icCross

        /**
         * Binds a row item to the ViewHolder
         */
        fun bind(position: Int) {
            val uploadableFile = uploadableFiles[position]
            val uri = uploadableFile.getMediaUri()
            background.setImageURI(Uri.fromFile(File(uri.toString())))
            if (position == callback.getCurrentSelectedFilePosition()) {
                val border = GradientDrawable()
                border.shape = GradientDrawable.RECTANGLE
                border.setStroke(BORDER_STROKE_WIDTH_PX, ContextCompat.getColor(itemView.context, R.color.primaryColor))
                rlContainer.isEnabled = true
                rlContainer.isClickable = true
                rlContainer.alpha = ALPHA_SELECTED
                rlContainer.background = border
                rlContainer.elevation = ELEVATION_SELECTED_DP
            } else {
                rlContainer.isEnabled = false
                rlContainer.isClickable = false
                rlContainer.alpha = ALPHA_UNSELECTED
                rlContainer.background = null
                rlContainer.elevation = ELEVATION_UNSELECTED_DP
            }

            ivCross.setOnClickListener {
                onThumbnailDeletedListener?.onThumbnailDeleted(position)
            }
        }
    }

    /**
     * Callback used to get the current selected file position
     */
    internal fun interface Callback {
        fun getCurrentSelectedFilePosition(): Int
    }

    /**
     * Interface to listen to thumbnail delete events
     */
    fun interface OnThumbnailDeletedListener {
        fun onThumbnailDeleted(position: Int)
    }
}
