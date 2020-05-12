package fr.free.nrw.commons.explore.depictions

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem


class DepictionAdapter(clickListener: (DepictedItem) -> Unit) :
    AsyncListDifferDelegationAdapter<DepictedItem>(
        DiffUtil,
        depictionDelegate(clickListener)
    ) {

    fun addAll(newResults: List<DepictedItem>) {
        items = (items ?: emptyList<DepictedItem>()) + newResults
    }

    fun clear() {
        items = emptyList()
    }

}

object DiffUtil : DiffUtil.ItemCallback<DepictedItem>() {
    override fun areItemsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
        oldItem == newItem
}
