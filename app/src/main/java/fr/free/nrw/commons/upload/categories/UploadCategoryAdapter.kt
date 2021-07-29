package fr.free.nrw.commons.upload.categories

import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.category.ExtendedCategoryClient
import org.jetbrains.annotations.NotNull

class UploadCategoryAdapter(
    onCategoryClicked: @NotNull() (CategoryItem) -> Unit,
    extendedCategoryClient: ExtendedCategoryClient
) :
    BaseDelegateAdapter<CategoryItem>(
        uploadCategoryDelegate(onCategoryClicked, extendedCategoryClient),
        areItemsTheSame = { oldItem, newItem -> oldItem.name == newItem.name },
        areContentsTheSame = { oldItem, newItem ->
            oldItem.name == newItem.name && oldItem.isSelected == newItem.isSelected
        }
    )


