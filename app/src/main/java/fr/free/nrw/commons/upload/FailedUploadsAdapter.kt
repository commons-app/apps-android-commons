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


class FailedUploadsAdapter(items: List<Contribution>) :
    RecyclerView.Adapter<FailedUploadsAdapter.ViewHolder>() {
    private val items: List<Contribution> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_failed_upload, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: Contribution = items[position]
        holder.titleTextView.setText(item.media.displayTitle)
        var imageRequest: ImageRequest? = null
        val imageSource: String = item.localUri.toString()

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

        if (item.state == Contribution.STATE_FAILED) {
            holder.errorTextView.setText("Failed")
            holder.errorTextView.visibility = View.VISIBLE
            holder.itemProgress.visibility = View.GONE
        }
        holder.itemImage.setImageRequest(imageRequest)
    }


    override fun getItemCount(): Int {
        return items.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemImage: com.facebook.drawee.view.SimpleDraweeView =
            itemView.findViewById(R.id.itemImage)
        var titleTextView: TextView = itemView.findViewById<TextView>(R.id.titleTextView)
        var itemProgress: ProgressBar = itemView.findViewById<ProgressBar>(R.id.itemProgress)
        var errorTextView: TextView = itemView.findViewById<TextView>(R.id.errorTextView)
        var deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    interface Callback {
        fun deleteUpload(contribution: Contribution?)
    }
}
