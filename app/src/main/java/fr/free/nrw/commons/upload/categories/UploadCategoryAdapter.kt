package fr.free.nrw.commons.upload.categories

import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.category.CategoryItem
import org.jetbrains.annotations.NotNull

class UploadCategoryAdapter(
    onCategoryClicked: @NotNull() (CategoryItem) -> Unit,
    categoryClient: CategoryClient
) :
    BaseDelegateAdapter<CategoryItem>(
        uploadCategoryDelegate(onCategoryClicked, categoryClient),
        areItemsTheSame = { oldItem, newItem -> oldItem.name == newItem.name },
        areContentsTheSame = { oldItem, newItem ->
            oldItem.name == newItem.name && oldItem.isSelected == newItem.isSelected
        }
    )


