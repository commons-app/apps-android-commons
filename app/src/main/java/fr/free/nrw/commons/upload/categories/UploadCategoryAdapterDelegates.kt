package fr.free.nrw.commons.upload.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import kotlinx.android.synthetic.main.layout_upload_categories_item.*

fun uploadCategoryDelegate(
    onCategoryClicked: (CategoryItem) -> Unit
) =
    adapterDelegateLayoutContainer<CategoryItem, CategoryItem>(
        R.layout.layout_upload_categories_item) {
        containerView.setOnClickListener {
            item.isSelected = !item.isSelected
            upload_category_checkbox.isChecked = item.isSelected
            onCategoryClicked(item)
        }

        bind {
            upload_category_checkbox.isChecked = item.isSelected
            category_label.text = item.name

            if(item.thumbnail != "null") {
                category_image.setImageURI(item.thumbnail)
            } else {
                category_image.setActualImageResource(R.drawable.commons)
            }

            if(item.description != "null") {
                category_description.text = item.description
            } else {
                category_description.text = ""
            }
        }
    }
