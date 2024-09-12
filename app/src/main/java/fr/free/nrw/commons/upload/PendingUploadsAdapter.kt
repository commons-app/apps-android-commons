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
import timber.log.Timber
import java.io.File

/**
 * Adapter for displaying pending uploads in a paginated list in PendingUploadsFragment. This adapter
 * binds data from [Contribution] objects to the item views in the RecyclerView, allowing users to
 * view details of pending uploads and perform actions such as deleting them.
 *
 * @param callback The callback to handle user actions such as Delete Uploads on pending uploads.
 */
class PendingUploadsAdapter(private val callback: Callback) :
    PagedListAdapter<Contribution, PendingUploadsAdapter.ViewHolder>(ContributionDiffCallback()) {

    /**
     * Creates a new ViewHolder instance. Inflates the layout for each item in the list.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_upload, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds data to the provided ViewHolder. Sets up the item view with data from the
     * contribution at the specified position utilizing payloads.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (val latestPayload = payloads.lastOrNull()) {
                is ContributionChangePayload.Progress -> holder.bindProgress(
                    latestPayload.transferred,
                    latestPayload.total,
                    getItem(position)!!.state
                )

                is ContributionChangePayload.State -> holder.bindState(latestPayload.state)
                else -> onBindViewHolder(holder, position)
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    /**
     * Binds data to the provided ViewHolder. Sets up the item view with data from the
     * contribution at the specified position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contribution = getItem(position)
        contribution?.let {
            holder.bind(it)
            holder.deleteButton.setOnClickListener {
                callback.deleteUpload(contribution)
            }
        }
    }

    /**
     * ViewHolder class for holding and binding item views.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemImage: com.facebook.drawee.view.SimpleDraweeView =
            itemView.findViewById(R.id.itemImage)
        var titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        var itemProgress: ProgressBar = itemView.findViewById(R.id.itemProgress)
        var errorTextView: TextView = itemView.findViewById(R.id.errorTextView)
        var deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(contribution: Contribution) {
            titleTextView.text = contribution.media.displayTitle

            val imageSource: String = contribution.localUri.toString()
            var imageRequest: ImageRequest? = null

            if (!TextUtils.isEmpty(imageSource)) {
                if (URLUtil.isFileUrl(imageSource)) {
                    imageRequest = ImageRequest.fromUri(Uri.parse(imageSource))
                } else {
                    val file = File(imageSource)
                    imageRequest = ImageRequest.fromFile(file)
                }
            }

            if (imageRequest != null) {
                itemImage.setImageRequest(imageRequest)
            }

            bindState(contribution.state)
            bindProgress(contribution.transferred, contribution.dataLength, contribution.state)
        }

        fun bindState(state: Int) {
            if (state == Contribution.STATE_QUEUED || state == Contribution.STATE_PAUSED) {
                errorTextView.text = "Queued"
                errorTextView.visibility = View.VISIBLE
                itemProgress.visibility = View.GONE
            } else {
                errorTextView.visibility = View.GONE
                itemProgress.visibility = View.VISIBLE
            }
        }

        fun bindProgress(transferred: Long, total: Long, state: Int) {
            if (transferred == 0L) {
                errorTextView.text = "Queued"
                errorTextView.visibility = View.VISIBLE
                itemProgress.visibility = View.GONE
            } else {
                if (state == Contribution.STATE_QUEUED || state == Contribution.STATE_PAUSED) {
                    errorTextView.text = "Queued"
                    errorTextView.visibility = View.VISIBLE
                    itemProgress.visibility = View.GONE
                } else {
                    errorTextView.visibility = View.GONE
                    itemProgress.visibility = View.VISIBLE
                    if (transferred >= total) {
                        itemProgress.isIndeterminate = true
                    } else {
                        itemProgress.isIndeterminate = false
                        itemProgress.progress =
                            ((transferred.toDouble() / total.toDouble()) * 100).toInt()
                    }
                }
            }
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
    }

    /**
     * Uses DiffUtil and payloads to calculate the changes in the list
     * It has methods that check pageId and the content of the items to determine if its a new item
     */
    class ContributionDiffCallback : DiffUtil.ItemCallback<Contribution>() {
        /**
         * Checks if two items represent the same contribution.
         * @param oldItem The old contribution item.
         * @param newItem The new contribution item.
         * @return True if the items are the same, false otherwise.
         */
        override fun areItemsTheSame(oldItem: Contribution, newItem: Contribution): Boolean {
            return oldItem.pageId.hashCode() == newItem.pageId.hashCode()
        }

        /**
         * Checks if the content of two items is the same.
         * @param oldItem The old contribution item.
         * @param newItem The new contribution item.
         * @return True if the contents are the same, false otherwise.
         */
        override fun areContentsTheSame(oldItem: Contribution, newItem: Contribution): Boolean {
            return oldItem.transferred == newItem.transferred
        }

        /**
         * Returns a payload representing the change between the old and new items.
         * @param oldItem The old contribution item.
         * @param newItem The new contribution item.
         * @return An object representing the change, or null if there are no changes.
         */
        override fun getChangePayload(oldItem: Contribution, newItem: Contribution): Any? {
            return when {
                oldItem.transferred != newItem.transferred -> {
                    ContributionChangePayload.Progress(newItem.transferred, newItem.dataLength)
                }

                oldItem.state != newItem.state -> {
                    ContributionChangePayload.State(newItem.state)
                }

                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }

    /**
     * Returns the unique item ID for the contribution at the specified position.
     * @param position The position of the item.
     * @return The unique item ID.
     */
    override fun getItemId(position: Int): Long {
        return getItem(position)?.pageId?.hashCode()?.toLong() ?: position.toLong()
    }

    /**
     * Sealed interface representing different types of changes to a contribution.
     */
    private sealed interface ContributionChangePayload {
        /**
         * Represents a change in the progress of a contribution.
         * @param transferred The amount of data transferred.
         * @param total The total amount of data.
         */
        data class Progress(val transferred: Long, val total: Long) : ContributionChangePayload

        /**
         * Represents a change in the state of a contribution.
         * @param state The state of the contribution.
         */
        data class State(val state: Int) : ContributionChangePayload
    }
}
