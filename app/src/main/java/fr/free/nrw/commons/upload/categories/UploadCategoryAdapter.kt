package fr.free.nrw.commons.upload.categories

import androidx.recyclerview.widget.DiffUtil
import fr.free.nrw.commons.category.CategoryItem

class UploadCategoryAdapter(onCategoryClicked: (CategoryItem) -> Unit) :
    BaseAdapter<CategoryItem>(
        object : DiffUtil.ItemCallback<CategoryItem>() {
            override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem) =
                oldItem.name == newItem.name && oldItem.isSelected == newItem.isSelected
        },
        uploadCategoryDelegate(onCategoryClicked)
    )


