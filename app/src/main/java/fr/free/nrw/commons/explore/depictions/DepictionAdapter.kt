package fr.free.nrw.commons.explore.depictions

import androidx.recyclerview.widget.DiffUtil
import fr.free.nrw.commons.upload.categories.BaseAdapter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem


class DepictionAdapter(clickListener: (DepictedItem) -> Unit) : BaseAdapter<DepictedItem>(
    object : DiffUtil.ItemCallback<DepictedItem>() {
        override fun areItemsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
            oldItem == newItem
    },
    depictionDelegate(clickListener)
)


