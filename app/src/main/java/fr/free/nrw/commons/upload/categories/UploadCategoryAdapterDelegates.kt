package fr.free.nrw.commons.upload.categories

import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.category.CategoryItem
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_upload_categories_item.*
import kotlinx.android.synthetic.main.layout_upload_depicts_item.*
import javax.inject.Inject

fun uploadCategoryDelegate(onCategoryClicked: (CategoryItem) -> Unit, categoryClient: CategoryClient) =
    adapterDelegateLayoutContainer<CategoryItem, CategoryItem>(R.layout.layout_upload_categories_item) {
        containerView.setOnClickListener {
            item.isSelected = !item.isSelected
            upload_category_checkbox.isChecked = item.isSelected
            onCategoryClicked(item)
        }

        bind {
            upload_category_checkbox.isChecked = item.isSelected
            Log.d("abcde", "call " + item.name);
            category_label.text = item.name
            val imageUrl: String = categoryClient
                .getCategoryThumbnail("Category:"+item.name)
                .subscribeOn(Schedulers.io())
                .blockingGet()
            category_image.setImageURI(Uri.parse(imageUrl))
        }
    }
