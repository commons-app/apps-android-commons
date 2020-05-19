package fr.free.nrw.commons.depictions.subClass

import fr.free.nrw.commons.explore.depictions.depictionDelegate
import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

class SubDepictionAdapter(clickListener: (DepictedItem) -> Unit) :
    BaseDelegateAdapter<DepictedItem>(
        depictionDelegate(clickListener),
        areItemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id }
    )
