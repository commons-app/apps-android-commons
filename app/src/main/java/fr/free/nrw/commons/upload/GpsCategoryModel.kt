package fr.free.nrw.commons.upload

import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsCategoryModel @Inject constructor() {
    val categoriesFromLocation = BehaviorSubject.createDefault(emptyList<String>())

    fun clear() {
        categoriesFromLocation.onNext(emptyList())
    }

    fun setCategoriesFromLocation(categoryList: List<String>) {
        categoriesFromLocation.onNext(categoryList)
    }
}
