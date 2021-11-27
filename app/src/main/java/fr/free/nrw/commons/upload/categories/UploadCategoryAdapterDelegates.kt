package fr.free.nrw.commons.upload.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.databinding.LayoutUploadCategoriesItemBinding

fun uploadCategoryDelegate(
    onCategoryClicked: (CategoryItem) -> Unit,
    existingCategories: List<String>
) =
    adapterDelegateViewBinding<CategoryItem, CategoryItem, LayoutUploadCategoriesItemBinding>({ layoutInflater, root ->
        LayoutUploadCategoriesItemBinding.inflate(layoutInflater, root, false)
    }) {

        binding.root.setOnClickListener {
            if(existingCategories.contains(item.name)){
                binding.uploadCategoryCheckbox.isChecked = true
            } else {
                item.isSelected = !item.isSelected
                binding.uploadCategoryCheckbox.isChecked = item.isSelected
                onCategoryClicked(item)
            }
        }
        bind {


            binding.uploadCategoryCheckbox.isChecked = item.isSelected
            binding.uploadCategoryCheckbox.text = item.name
            if(existingCategories.contains(item.name)){
                binding.uploadCategoryCheckbox.isChecked = true
            }
        }
    }
