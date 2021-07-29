package fr.free.nrw.commons.upload.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.category.ExtendedCategoryClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_upload_categories_item.*
import org.wikipedia.dataclient.mwapi.MwQueryPage

private val compositeDisposable = CompositeDisposable()

fun uploadCategoryDelegate(
    onCategoryClicked: (CategoryItem) -> Unit,
    extendedCategoryClient: ExtendedCategoryClient
) =
    adapterDelegateLayoutContainer<CategoryItem, CategoryItem>(
        R.layout.layout_upload_categories_item) {
        containerView.setOnClickListener {
            item.isSelected = !item.isSelected
            upload_category_checkbox.isChecked = item.isSelected
            onCategoryClicked(item)
        }

        bind {
            upload_category_checkbox.isChecked = item.isSelected
            category_label.text = item.name

            compositeDisposable.add(
                extendedCategoryClient.getCategoryThumbnail(
                    "Category:" + item.name
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { s: MwQueryPage? ->
                        category_image.setImageURI(s?.thumbUrl())
                    }
            )
        }
    }
