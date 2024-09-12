package fr.free.nrw.commons.upload

import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import java.io.File

/**
 * Adapter for displaying failed uploads in a paginated list in FailedUploadsFragment. This adapter
 * binds the data from [Contribution] objects to the item views in the RecyclerView, allowing users to view
 * details of failed uploads, retry them, or delete them.
 *
 * @param callback The callback to handle user actions such as Delete Uploads and Restart Uploads
 * on failed uploads.
 */
class FailedUploadsAdapter(callback: Callback) :
    PagedListAdapter<Contribution, FailedUploadsAdapter.ViewHolder>(ContributionDiffCallback()) {
    private var callback: Callback = callback

    /**
     * Creates a new ViewHolder instance. Inflates the layout for each item in the list.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_failed_upload, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds data to the provided ViewHolder. Sets up the item view with data from the
     * contribution at the specified position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: Contribution? = getItem(position)
        if (item != null) {
            holder.titleTextView.setText(item.media.displayTitle)
        }
        var imageRequest: ImageRequest? = null
        val imageSource: String = item?.localUri.toString()

        if (!TextUtils.isEmpty(imageSource)) {
            if (URLUtil.isFileUrl(imageSource)) {
                imageRequest = ImageRequest.fromUri(Uri.parse(imageSource))!!
            } else if (imageSource != null) {
                val file = File(imageSource)
                imageRequest = ImageRequest.fromFile(file)!!
            }

            if (imageRequest != null) {
                holder.itemImage.setImageRequest(imageRequest)
            }
        }

        if (item != null) {
            if (item.state == Contribution.STATE_FAILED) {
                if (item.errorInfo != null) {
                    holder.errorTextView.setText(item.errorInfo)
                } else {
                    holder.errorTextView.setText("Failed")
                }
                holder.errorTextView.visibility = View.VISIBLE
                holder.itemProgress.visibility = View.GONE
            }
        }
        holder.deleteButton.setOnClickListener {
            callback.deleteUpload(item)
        }
        holder.retryButton.setOnClickListener {
            callback.restartUpload(position)
        }
        holder.itemImage.setImageRequest(imageRequest)
    }

    /**
     * ViewHolder for the failed upload item. Holds references to the views for each item.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemImage: com.facebook.drawee.view.SimpleDraweeView =
            itemView.findViewById(R.id.itemImage)
        var titleTextView: TextView = itemView.findViewById<TextView>(R.id.titleTextView)
        var itemProgress: ProgressBar = itemView.findViewById<ProgressBar>(R.id.itemProgress)
        var errorTextView: TextView = itemView.findViewById<TextView>(R.id.errorTextView)
        var deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        var retryButton: ImageView = itemView.findViewById(R.id.retryButton)
    }

    /**
     * Returns the ID of the item at the specified position. Uses the pageId of the contribution
     * for unique identification.
     */
    override fun getItemId(position: Int): Long {
        return getItem(position)?.pageId?.hashCode()?.toLong() ?: position.toLong()
    }

    /**
     * Uses DiffUtil to calculate the changes in the list
     * It has methods that check pageId and the content of the items to determine if its a new item
     */
    class ContributionDiffCallback : DiffUtil.ItemCallback<Contribution>() {
        override fun areItemsTheSame(oldItem: Contribution, newItem: Contribution): Boolean {
            return oldItem.pageId.hashCode() == newItem.pageId.hashCode()
        }

        override fun areContentsTheSame(oldItem: Contribution, newItem: Contribution): Boolean {
            return oldItem.transferred == newItem.transferred
        }
    }

    /**
     * Callback interface for handling actions related to failed uploads.
     */
    interface Callback {
        /**
         * Deletes the failed upload item.
         *
         * @param contribution to be deleted.
         */
        fun deleteUpload(contribution: Contribution?)

        /**
         * Restarts the upload for the item at the specified index.
         *
         * @param index The position of the item in the list.
         */
        fun restartUpload(index: Int)
    }
}
