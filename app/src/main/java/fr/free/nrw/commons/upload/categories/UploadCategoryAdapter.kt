package fr.free.nrw.commons.upload.categories

import fr.free.nrw.commons.category.CategoryItem

class UploadCategoryAdapter(onCategoryClicked: (CategoryItem) -> Unit) :
    BaseDelegateAdapter<CategoryItem>(
        uploadCategoryDelegate(onCategoryClicked),
        areItemsTheSame = { oldItem, newItem -> oldItem.name == newItem.name },
        areContentsTheSame = { oldItem, newItem ->
            oldItem.name == newItem.name && oldItem.isSelected == newItem.isSelected
        }
    )


