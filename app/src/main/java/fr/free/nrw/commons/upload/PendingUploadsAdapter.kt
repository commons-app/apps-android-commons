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
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import timber.log.Timber
import java.io.File


class PendingUploadsAdapter(items: List<Contribution>, callback: Callback) :
    RecyclerView.Adapter<PendingUploadsAdapter.ViewHolder>() {
    private val items: List<Contribution> = items
    private var callback:Callback = callback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_pending_upload, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: Contribution = items[position]
        holder.titleTextView.setText(item.media.displayTitle)
        var imageRequest: ImageRequest? = null

        val imageSource: String = item.localUri.toString()
        Timber.tag("PRINT").e("--"+imageSource)

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

        if (item.state == Contribution.STATE_QUEUED || item.state == Contribution.STATE_PAUSED) {
            holder.errorTextView.setText("Queued")
            holder.errorTextView.visibility = View.VISIBLE
            holder.itemProgress.visibility = View.GONE
        } else {
            holder.errorTextView.visibility = View.GONE
            holder.itemProgress.visibility = View.VISIBLE
            val total: Long = item.dataLength
            val transferred: Long = item.transferred
            if (transferred == 0L || transferred >= total) {
                holder.itemProgress.setIndeterminate(true)
            } else {
                holder.itemProgress.setIndeterminate(false)
                holder.itemProgress.setProgress(((transferred.toDouble() / total.toDouble()) * 100).toInt())
            }
        }

        holder.itemImage.setImageRequest(imageRequest)

        holder.deleteButton.setOnClickListener{
            callback!!.deleteUpload(item)
        }
    }



    override fun getItemCount(): Int {
        return items.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemImage: com.facebook.drawee.view.SimpleDraweeView = itemView.findViewById(R.id.itemImage)
        var titleTextView: TextView = itemView.findViewById<TextView>(R.id.titleTextView)
        var itemProgress: ProgressBar = itemView.findViewById<ProgressBar>(R.id.itemProgress)
        var errorTextView: TextView = itemView.findViewById<TextView>(R.id.errorTextView)
        var deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    interface Callback {
        fun deleteUpload(contribution: Contribution?)
    }
}
