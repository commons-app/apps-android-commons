package fr.free.nrw.commons.upload.categories

import fr.free.nrw.commons.category.CategoryItem
import org.jetbrains.annotations.NotNull

class UploadCategoryAdapter(
    onCategoryClicked: @NotNull() (CategoryItem) -> Unit, nearbyPlaceCategory: String?) :
    BaseDelegateAdapter<CategoryItem>(
        uploadCategoryDelegate(onCategoryClicked, nearbyPlaceCategory),
        areItemsTheSame = { oldItem, newItem -> oldItem.name == newItem.name },
        areContentsTheSame = { oldItem, newItem ->
            oldItem.name == newItem.name && oldItem.isSelected == newItem.isSelected
        }
    )


