package fr.free.nrw.commons.upload.depicts

import android.net.Uri
import android.text.TextUtils
import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import kotlinx.android.synthetic.main.layout_upload_depicts_item.*


fun uploadDepictsDelegate(onDepictClicked: (DepictedItem) -> Unit) =
    adapterDelegateLayoutContainer<DepictedItem, DepictedItem>(R.layout.layout_upload_depicts_item) {
        val onClickListener = { _: View? ->
            item.isSelected = !item.isSelected
            depict_checkbox.isChecked = item.isSelected
            onDepictClicked(item)
        }
        containerView.setOnClickListener(onClickListener)
        depict_checkbox.setOnClickListener(onClickListener)
        bind {
            depict_checkbox.isChecked = item.isSelected
            depicts_label.text = item.name
            description.text = item.description
            val imageUrl = item.imageUrl
            if (TextUtils.isEmpty(imageUrl)) {
                depicted_image.setActualImageResource(R.drawable.ic_wikidata_logo_24dp)
            } else {
                depicted_image.setImageURI(Uri.parse(imageUrl))
            }
        }
    }
