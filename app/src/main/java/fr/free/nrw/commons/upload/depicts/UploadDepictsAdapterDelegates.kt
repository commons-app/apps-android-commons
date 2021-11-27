package fr.free.nrw.commons.upload.depicts

import android.net.Uri
import android.text.TextUtils
import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.LayoutUploadDepictsItemBinding
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem


fun uploadDepictsDelegate(onDepictClicked: (DepictedItem) -> Unit) =
    adapterDelegateViewBinding<DepictedItem, DepictedItem, LayoutUploadDepictsItemBinding>({ layoutInflater, parent ->
        LayoutUploadDepictsItemBinding.inflate(layoutInflater, parent, false)
    }) {
        val onClickListener = { _: View? ->
            item.isSelected = !item.isSelected
            binding.depictCheckbox.isChecked = item.isSelected
            onDepictClicked(item)
        }
        binding.root.setOnClickListener(onClickListener)
        binding.depictCheckbox.setOnClickListener(onClickListener)
        bind {
            binding.depictCheckbox.isChecked = item.isSelected
            binding.depictsLabel.text = item.name
            binding.description.text = item.description
            val imageUrl = item.imageUrl
            if (TextUtils.isEmpty(imageUrl)) {
                binding.depictedImage.setActualImageResource(R.drawable.ic_wikidata_logo_24dp)
            } else {
                binding.depictedImage.setImageURI(Uri.parse(imageUrl))
            }
        }
    }
