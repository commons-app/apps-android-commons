package fr.free.nrw.commons.upload.categories

import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.databinding.LayoutUploadCategoriesItemBinding

fun uploadCategoryDelegate(onCategoryClicked: (CategoryItem) -> Unit) =
    adapterDelegateViewBinding<CategoryItem, CategoryItem,
            LayoutUploadCategoriesItemBinding>({ layoutInflater, root ->
        LayoutUploadCategoriesItemBinding.inflate(layoutInflater, root, false)
    }) {
        val onClickListener = { _: View? ->
            item.isSelected = !item.isSelected
            binding.uploadCategoryCheckbox.isChecked = item.isSelected
            onCategoryClicked(item)
        }

        binding.root.setOnClickListener(onClickListener)
        binding.uploadCategoryCheckbox.setOnClickListener(onClickListener)

        bind {
            binding.uploadCategoryCheckbox.isChecked = item.isSelected
            binding.categoryLabel.text = item.name
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
