package fr.free.nrw.commons.category

import android.text.TextUtils
import fr.free.nrw.commons.upload.GpsCategoryModel
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.StringSortingUtils
import io.reactivex.Observable
import io.reactivex.functions.Function4

/**
 * Loads the categories offered while uploading a picture.
 */
internal class CategorySuggestions(
    private val categoryClient: CategoryClient,
    private val categoryDao: CategoryDao,
    private val gpsCategoryModel: GpsCategoryModel,
) {
    /**
     * Returns category suggestions for an upload.
     *
     * An empty [term] combines depiction, location, title, and recent categories.
     * Any other term searches Commons for categories that start with that prefix.
     */
    fun suggestionsOrSearch(
        term: String,
        imageTitleList: List<String>,
        selectedDepictions: List<DepictedItem>,
    ): Observable<List<CategoryItem>> =
        if (TextUtils.isEmpty(term)) {
            Observable.combineLatest(
                categoriesFromDepiction(selectedDepictions),
                gpsCategoryModel.categoriesFromLocation,
                titleCategories(imageTitleList),
                Observable.just(categoryDao.recentCategories(CategoriesModel.SEARCH_CATS_LIMIT)),
                Function4(::combine),
            )
        } else {
            categoryClient
                .searchCategoriesForPrefix(term, CategoriesModel.SEARCH_CATS_LIMIT)
                .map { it.sortedWith(StringSortingUtils.sortBySimilarity(term)) }
                .toObservable()
        }

    /**
     * Fetches details of every category associated with selected depictions, converts them into
     * CategoryItem and returns them in a list.
     * If a selected depiction has no categories, the categories in which its P18 belongs are
     * returned in the list.
     *
     * @param selectedDepictions selected DepictItems
     * @return List of CategoryItem associated with selected depictions
     */
    private fun categoriesFromDepiction(
        selectedDepictions: List<DepictedItem>,
    ): Observable<MutableList<CategoryItem>>? {
        val observables =
            selectedDepictions.map { depictedItem ->
                if (depictedItem.commonsCategories.isEmpty()) {
                    if (depictedItem.primaryImage == null) {
                        return@map Observable.just(emptyList<CategoryItem>())
                    }
                    Observable
                        .just(depictedItem.primaryImage)
                        .map { image ->
                            categoryClient
                                .getCategoriesOfImage(image, CategoriesModel.SEARCH_CATS_LIMIT)
                                .map {
                                    it.map { category ->
                                        CategoryItem(
                                            category.name,
                                            category.description,
                                            category.thumbnail,
                                            category.isSelected,
                                        )
                                    }
                                }.blockingGet()
                        }.flatMapIterable { it }
                        .toList()
                        .toObservable()
                } else {
                    Observable
                        .fromIterable(depictedItem.commonsCategories)
                        .map { categoryItem ->
                            categoryClient
                                .getCategoriesByName(
                                    categoryItem.name,
                                    categoryItem.name,
                                    CategoriesModel.SEARCH_CATS_LIMIT,
                                ).map {
                                    CategoryItem(
                                        it[0].name,
                                        it[0].description,
                                        it[0].thumbnail,
                                        it[0].isSelected,
                                    )
                                }.blockingGet()
                        }.toList()
                        .toObservable()
                }
            }
        return Observable
            .concat(observables)
            .scan(mutableListOf<CategoryItem>()) { accumulator, currentList ->
                accumulator.apply { addAll(currentList) }
            }
    }

    /**
     * Appends depiction, location, title, and recent categories into one list.
     */
    private fun combine(
        depictionCategories: List<CategoryItem>,
        locationCategories: List<CategoryItem>,
        titles: List<CategoryItem>,
        recents: List<CategoryItem>,
    ) = depictionCategories + locationCategories + titles + recents

    /**
     * Returns title based categories
     * @param titleList
     * @return
     */
    private fun titleCategories(titleList: List<String>) =
        if (titleList.isNotEmpty()) {
            Observable.combineLatest(titleList.map { getTitleCategories(it) }) { searchResults ->
                searchResults.map { it as List<CategoryItem> }.flatten()
            }
        } else {
            Observable.just(emptyList())
        }

    /**
     * Return category for single title
     * @param title
     * @return
     */
    private fun getTitleCategories(title: String): Observable<List<CategoryItem>> =
        categoryClient.searchCategories(title, CategoriesModel.SEARCH_CATS_LIMIT).toObservable()
}
