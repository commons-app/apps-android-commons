package fr.free.nrw.commons.explore.images

import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import kotlinx.android.synthetic.main.layout_category_images.*


fun searchImagesAdapter(onImageClicked: (Media) -> Unit) =
    adapterDelegateLayoutContainer<Media, Media>(R.layout.layout_category_images) {
        categoryImageView.setOnClickListener { onImageClicked(item) }
        bind {
            categoryImageTitle.text = item.thumbnailTitle
            categoryImageView.setImageURI(item.thumbUrl)
            if (item.creator?.isNotEmpty() == true) {
                categoryImageAuthor.visibility = View.VISIBLE
                categoryImageAuthor.text = getString(R.string.image_uploaded_by, item.creator)
            } else {
                categoryImageAuthor.visibility = View.GONE
            }
        }

    }
