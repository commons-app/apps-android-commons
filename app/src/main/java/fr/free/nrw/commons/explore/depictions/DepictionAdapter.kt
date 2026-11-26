package fr.free.nrw.commons.explore.depictions

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ItemDepictionsBinding
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import java.io.File

class DepictionAdapter(
    private val onDepictionClicked: (DepictedItem) -> Unit,
) : PagedListAdapter<DepictedItem, DepictedItemViewHolder>(DepictionDiffUtilCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = DepictedItemViewHolder(
        ItemDepictionsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(
        holder: DepictedItemViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position)!!, onDepictionClicked)
    }
}

class DepictedItemViewHolder(
    private val binding: ItemDepictionsBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: DepictedItem,
        onDepictionClicked: (DepictedItem) -> Unit,
    ) = with(binding) {
        root.setOnClickListener { onDepictionClicked(item) }
        depictsLabel.text = item.name
        description.text = item.description
        if (item.thumbnailUrl?.isNotBlank() == true) {
            val imageSource = item.thumbnailUrl
            var imageRequest: ImageRequest? = null
            if (URLUtil.isHttpsUrl(imageSource)) {
                imageRequest =
                    ImageRequestBuilder
                        .newBuilderWithSource(Uri.parse(imageSource))
                        .setProgressiveRenderingEnabled(true)
                        .build()
            } else if (URLUtil.isFileUrl(imageSource)) {
                imageRequest = ImageRequest.fromUri(Uri.parse(imageSource))
            } else if (imageSource != null) {
                val file = File(imageSource)
                imageRequest = ImageRequest.fromFile(file)
            }

            if (imageRequest != null) {
                depictsImage.setImageRequest(imageRequest)
            }
        } else {
            depictsImage.setActualImageResource(R.drawable.ic_wikidata_logo_24dp)
        }
    }
}

private object DepictionDiffUtilCallback : DiffUtil.ItemCallback<DepictedItem>() {
    override fun areItemsTheSame(
        oldItem: DepictedItem,
        newItem: DepictedItem,
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: DepictedItem,
        newItem: DepictedItem,
    ) = oldItem == newItem
}
