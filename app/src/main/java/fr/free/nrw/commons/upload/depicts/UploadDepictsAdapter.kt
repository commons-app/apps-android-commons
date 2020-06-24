package fr.free.nrw.commons.upload.depicts

import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

class UploadDepictsAdapter(onDepictsClicked: (DepictedItem) -> Unit) :
    BaseDelegateAdapter<DepictedItem>(
        uploadDepictsDelegate(onDepictsClicked),
        areItemsTheSame = { oldItem, newItem -> oldItem.id == newItem.id }
    )
