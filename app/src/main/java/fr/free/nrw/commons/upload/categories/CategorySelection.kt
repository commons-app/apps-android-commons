package fr.free.nrw.commons.upload.categories

import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository

/** Applies suggested category selections and immediately updates the displayed list. */
class CategorySelection(
    private val repository: UploadRepository,
) {
    /**
     * Selects each [CategoryItem] in a given list as if they were clicked by the user by calling
     * [onCategoryItemClicked] for each category and adding the category to [categoryList]
     */
    fun selectNewCategories(
        toSelect: List<CategoryItem>,
        media: Media?,
        categoryList: MutableLiveData<List<CategoryItem>>,
    ) {
        toSelect.forEach {
            it.isSelected = true
            repository.onCategoryClicked(it, media)
        }

        // Add the new selections to the list of category items so that the selections appear
        // immediately (i.e. without any search term queries)
        categoryList.value
            ?.toMutableList()
            ?.let { toSelect + it }
            ?.distinctBy(CategoryItem::name)
            ?.let { categoryList.postValue(it) }
    }
}
