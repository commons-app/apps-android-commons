package fr.free.nrw.commons.explore.images

import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import fr.free.nrw.commons.Media

class SearchImagesAdapter(onImageClicked: (Media) -> Unit) : ListDelegationAdapter<List<Media>>(
    searchImagesAdapter(onImageClicked)
) {
    fun getItemAt(position: Int) = items[position]

    init {
        items = emptyList()
    }

    fun clear() {
        items = emptyList()
    }

    fun addAll(mediaList: List<Media>) {
        items = items + mediaList
    }

    fun updateThumbnail(position: Int, thumbnailTitle: String) {
        items[position].thumbnailTitle = thumbnailTitle
        notifyItemChanged(position)
    }
}
