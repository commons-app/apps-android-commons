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


class PendingUploadsAdapter(private val callback: Callback) :
    PagedListAdapter<Contribution, PendingUploadsAdapter.ViewHolder>(ContributionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_upload, parent, false)
        return ViewHolder(view)
    }

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contribution = getItem(position)
        contribution?.let {
            holder.bind(it)
            holder.deleteButton.setOnClickListener {
                callback.deleteUpload(contribution)
            }
        }
    }

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
            Timber.tag("PRINT").e("State is: "+state)
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
            Timber.tag("PRINT").e("State is2: "+state)
            if (transferred == 0L) {
                errorTextView.text = "Queued"
                errorTextView.visibility = View.VISIBLE
                itemProgress.visibility = View.GONE
            } else {
                if (state == Contribution.STATE_QUEUED || state == Contribution.STATE_PAUSED){
                    errorTextView.text = "Queued"
                    errorTextView.visibility = View.VISIBLE
                    itemProgress.visibility = View.GONE
                } else{
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

    interface Callback {
        fun deleteUpload(contribution: Contribution?)
    }

    class ContributionDiffCallback : DiffUtil.ItemCallback<Contribution>() {
        override fun areItemsTheSame(oldItem: Contribution, newItem: Contribution): Boolean {
            return oldItem.pageId.hashCode() == newItem.pageId.hashCode()
        }

        override fun areContentsTheSame(oldItem: Contribution, newItem: Contribution): Boolean {
            return oldItem.transferred == newItem.transferred
        }

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

    override fun getItemId(position: Int): Long {
        return getItem(position)?.pageId?.hashCode()?.toLong() ?: position.toLong()
    }

    private sealed interface ContributionChangePayload {
        data class Progress(val transferred: Long, val total: Long) : ContributionChangePayload
        data class State(val state: Int) : ContributionChangePayload
    }
}
