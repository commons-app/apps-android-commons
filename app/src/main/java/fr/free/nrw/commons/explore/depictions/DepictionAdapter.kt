package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem


class DepictionAdapter(clickListener: (DepictedItem) -> Unit) : BaseDelegateAdapter<DepictedItem>(
    depictionDelegate(clickListener),
    areItemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id }
)


