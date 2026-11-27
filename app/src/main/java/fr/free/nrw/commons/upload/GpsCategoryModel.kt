package fr.free.nrw.commons.upload

import fr.free.nrw.commons.category.CategoryItem
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsCategoryModel
    @Inject
    constructor() {
        val categoriesFromLocation = BehaviorSubject.createDefault(emptyList<CategoryItem>())

        @Synchronized
        fun clear() {
            categoriesFromLocation.onNext(emptyList())
        }

        /** Adds a place's categories, as the pictures of one upload can be taken at several places. */
        @Synchronized
        fun addCategoriesFromLocation(categoryList: List<CategoryItem>) {
            val all = categoriesFromLocation.value.orEmpty() + categoryList
            categoriesFromLocation.onNext(all.distinctBy { it.name })
        }
    }
