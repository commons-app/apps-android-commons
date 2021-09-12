package fr.free.nrw.commons.upload

import fr.free.nrw.commons.category.CategoryItem
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsCategoryModel @Inject constructor() {
    val categoriesFromLocation = BehaviorSubject.createDefault(emptyList<CategoryItem>())

    fun clear() {
        categoriesFromLocation.onNext(emptyList())
    }

    fun setCategoriesFromLocation(categoryList: List<CategoryItem>) {
        categoriesFromLocation.onNext(categoryList)
    }
}
