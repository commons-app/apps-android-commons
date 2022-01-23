package fr.free.nrw.commons.explore.depictions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ItemDepictionsBinding
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

class DepictionAdapter(private val onDepictionClicked: (DepictedItem) -> Unit) :
    PagedListAdapter<DepictedItem, DepictedItemViewHolder>(DepictionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DepictedItemViewHolder(
        ItemDepictionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: DepictedItemViewHolder, position: Int) {
        holder.bind(getItem(position)!!, onDepictionClicked)
    }
}

class DepictedItemViewHolder(
    private val binding: ItemDepictionsBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: DepictedItem, onDepictionClicked: (DepictedItem) -> Unit) = with(binding) {
        root.setOnClickListener { onDepictionClicked(item) }
        depictsLabel.text = item.name
        description.text = item.description
        if (item.imageUrl?.isNotBlank() == true) {
            depictsImage.setImageURI(item.imageUrl)
        } else {
            depictsImage.setActualImageResource(R.drawable.ic_wikidata_logo_24dp)
        }
    }
}

private object DepictionDiffUtilCallback : DiffUtil.ItemCallback<DepictedItem>() {
    override fun areItemsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: DepictedItem, newItem: DepictedItem) =
        oldItem == newItem
}
