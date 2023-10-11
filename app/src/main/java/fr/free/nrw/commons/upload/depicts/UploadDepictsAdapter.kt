package fr.free.nrw.commons.upload.depicts

import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

class UploadDepictsAdapter(onDepictsClicked: (DepictedItem) -> Unit, nearbyPlace: Place?) :
    BaseDelegateAdapter<DepictedItem>(
        uploadDepictsDelegate(onDepictsClicked, nearbyPlace),
        areItemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id },
        areContentsTheSame = { itemA, itemB -> itemA.isSelected == itemB.isSelected}
    )
