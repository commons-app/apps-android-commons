package fr.free.nrw.commons.explore.depictions

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import kotlinx.android.synthetic.main.item_depictions.*


fun depictionDelegate(onDepictionClicked: (DepictedItem) -> Unit) =
    adapterDelegateLayoutContainer<DepictedItem, DepictedItem>(R.layout.item_depictions) {
        containerView.setOnClickListener { onDepictionClicked(item) }
        bind {
            depicts_label.text = item.name
            description.text = item.description
            if (item.imageUrl?.isNotBlank() == true) {
                depicts_image.setImageURI(item.imageUrl)
            } else {
                depicts_image.setActualImageResource(R.drawable.ic_wikidata_logo_24dp)
            }
        }
    }
