package fr.free.nrw.commons.explore.depictions

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.BaseViewHolder
import fr.free.nrw.commons.explore.inflate
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import kotlinx.android.synthetic.main.item_depictions.*


class DepictionAdapter(val onDepictionClicked: (DepictedItem) -> Unit) :
    PagedListAdapter<DepictedItem, DepictedItemViewHolder>(
        object : DiffUtil.ItemCallback<DepictedItem>() {
            override fun areItemsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
                oldItem == newItem
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepictedItemViewHolder {
        return DepictedItemViewHolder(parent.inflate(R.layout.item_depictions), onDepictionClicked)
    }

    override fun onBindViewHolder(holder: DepictedItemViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }
}

class DepictedItemViewHolder(containerView: View, val onDepictionClicked: (DepictedItem) -> Unit) :
    BaseViewHolder<DepictedItem>(containerView) {

    override fun bind(item: DepictedItem) {
        containerView.setOnClickListener { onDepictionClicked(item) }
        depicts_label.text = item.name
        description.text = item.description
        if (item.imageUrl?.isNotBlank() == true) {
            depicts_image.setImageURI(item.imageUrl)
        } else {
            depicts_image.setActualImageResource(R.drawable.ic_wikidata_logo_24dp)
        }
    }
}




