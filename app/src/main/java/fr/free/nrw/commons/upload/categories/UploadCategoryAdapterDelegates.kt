package fr.free.nrw.commons.upload.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.databinding.LayoutUploadCategoriesItemBinding

fun uploadCategoryDelegate(onCategoryClicked: (CategoryItem) -> Unit) =
    adapterDelegateViewBinding<CategoryItem, CategoryItem, LayoutUploadCategoriesItemBinding>({ layoutInflater, root ->
        LayoutUploadCategoriesItemBinding.inflate(layoutInflater, root, false)
    }) {
        binding.root.setOnClickListener {
            item.isSelected = !item.isSelected
            binding.uploadCategoryCheckbox.isChecked = item.isSelected
            onCategoryClicked(item)
        }
        bind {
            binding.uploadCategoryCheckbox.isChecked = item.isSelected
            binding.uploadCategoryCheckbox.text = item.name
        }
    }
