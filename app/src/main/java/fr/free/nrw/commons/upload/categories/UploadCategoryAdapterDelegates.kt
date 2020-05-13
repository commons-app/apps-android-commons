package fr.free.nrw.commons.upload.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import kotlinx.android.synthetic.main.layout_upload_categories_item.*

fun uploadCategoryDelegate(onCategoryClicked: (CategoryItem) -> Unit) =
    adapterDelegateLayoutContainer<CategoryItem, CategoryItem>(R.layout.layout_upload_categories_item) {
        containerView.setOnClickListener {
            item.isSelected = !item.isSelected
            uploadCategoryCheckbox.isChecked = item.isSelected
            onCategoryClicked(item)
        }
        bind {
            uploadCategoryCheckbox.isChecked = item.isSelected
            uploadCategoryCheckbox.text = item.name
        }
    }
