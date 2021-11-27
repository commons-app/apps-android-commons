package fr.free.nrw.commons.upload.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.R
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
            if(item.thumbnail != "null") {
                binding.categoryImage.setImageURI(item.thumbnail)
            } else {
                binding.categoryImage.setActualImageResource(R.drawable.commons)
            }

            if(item.description != "null") {
                binding.categoryDescription.text = item.description
            } else {
                binding.categoryDescription.text = ""
            }
        }
    }
