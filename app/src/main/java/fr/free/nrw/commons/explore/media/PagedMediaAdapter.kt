package fr.free.nrw.commons.explore.media

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.paging.BaseViewHolder
import fr.free.nrw.commons.explore.paging.inflate
import kotlinx.android.synthetic.main.layout_category_images.*

class PagedMediaAdapter(private val onImageClicked: (Int) -> Unit) :
    PagedListAdapter<Media, SearchImagesViewHolder>(object : DiffUtil.ItemCallback<Media>() {
        override fun areItemsTheSame(oldItem: Media, newItem: Media) =
            oldItem.pageId == newItem.pageId

        override fun areContentsTheSame(oldItem: Media, newItem: Media) =
            oldItem.pageId == newItem.pageId
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SearchImagesViewHolder(
            parent.inflate(R.layout.layout_category_images),
            onImageClicked
        )

    override fun onBindViewHolder(holder: SearchImagesViewHolder, position: Int) {
        holder.bind(getItem(position)!! to position)
    }
}

class SearchImagesViewHolder(containerView: View, val onImageClicked: (Int) -> Unit) :
    BaseViewHolder<Pair<Media, Int>>(containerView) {
    override fun bind(item: Pair<Media, Int>) {
        val media = item.first
        categoryImageView.setOnClickListener { onImageClicked(item.second) }
        categoryImageTitle.text = media.mostRelevantCaption
        categoryImageView.setImageURI(media.thumbUrl)
        if (media.author?.isNotEmpty() == true) {
            categoryImageAuthor.visibility = View.VISIBLE
            categoryImageAuthor.text =
                containerView.context.getString(R.string.image_uploaded_by, media.user)
        } else {
            categoryImageAuthor.visibility = View.GONE
        }
    }

}
