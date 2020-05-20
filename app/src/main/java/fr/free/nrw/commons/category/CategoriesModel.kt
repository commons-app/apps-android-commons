package fr.free.nrw.commons.category

import android.text.TextUtils
import fr.free.nrw.commons.upload.GpsCategoryModel
import fr.free.nrw.commons.utils.StringSortingUtils
import io.reactivex.Observable
import io.reactivex.functions.Function3
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * The model class for categories in upload
 */
class CategoriesModel @Inject constructor(
    private val categoryClient: CategoryClient,
    private val categoryDao: CategoryDao,
    private val gpsCategoryModel: GpsCategoryModel
) {
    private val selectedCategories: MutableList<CategoryItem> = mutableListOf()

    /**
     * Returns if the item contains an year
     * @param item
     * @return
     */
    fun containsYear(item: String): Boolean {
        //Check for current and previous year to exclude these categories from removal
        val now = Calendar.getInstance()
        val year = now[Calendar.YEAR]
        val yearInString = year.toString()
        val prevYear = year - 1
        val prevYearInString = prevYear.toString()
        Timber.d("Previous year: %s", prevYearInString)

        //Check if item contains a 4-digit word anywhere within the string (.* is wildcard)
        //And that item does not equal the current year or previous year
        //And if it is an irrelevant category such as Media_needing_categories_as_of_16_June_2017(Issue #750)
        //Check if the year in the form of XX(X)0s is relevant, i.e. in the 2000s or 2010s as stated in Issue #1029
        return item.matches(".*(19|20)\\d{2}.*".toRegex())
                && !item.contains(yearInString)
                && !item.contains(prevYearInString)
                || item.matches("(.*)needing(.*)".toRegex())
                || item.matches("(.*)taken on(.*)".toRegex())
                || item.matches(".*0s.*".toRegex())
                && !item.matches(".*(200|201)0s.*".toRegex())
    }

    /**
     * Updates category count in category dao
     * @param item
     */
    fun updateCategoryCount(item: CategoryItem) {
        var category = categoryDao.find(item.name)

        // Newly used category...
        if (category == null) {
            category = Category(null, item.name, Date(), 0)
        }
        category.incTimesUsed()
        categoryDao.save(category)
    }

    /**
     * Regional category search
     * @param term
     * @param imageTitleList
     * @return
     */
    fun searchAll(term: String, imageTitleList: List<String>): Observable<List<CategoryItem>> {
        return suggestionsOrSearch(term, imageTitleList)
            .map { it.map { CategoryItem(it, false) } }
    }

    private fun suggestionsOrSearch(term: String, imageTitleList: List<String>):
            Observable<List<String>> {
        return if (TextUtils.isEmpty(term))
            Observable.combineLatest(
                gpsCategoryModel.categoriesFromLocation,
                titleCategories(imageTitleList),
                Observable.just(categoryDao.recentCategories(SEARCH_CATS_LIMIT)),
                Function3(::combine)
            )
        else
            categoryClient.searchCategoriesForPrefix(term.toLowerCase(), SEARCH_CATS_LIMIT)
                .map { it.sortedWith(StringSortingUtils.sortBySimilarity(term)) }
    }

    private fun combine(
        locationCategories: List<String>,
        titles: List<String>,
        recents: List<String>
    ) = locationCategories + titles + recents


    /**
     * Returns title based categories
     * @param titleList
     * @return
     */
    private fun titleCategories(titleList: List<String>): Observable<List<String>> {
        return if (titleList.isNotEmpty())
            Observable.combineLatest(titleList.map { getTitleCategories(it) }) { searchResults ->
                searchResults.map { it as List<String> }.flatten()
            }
        else
            Observable.just(emptyList())
    }

    /**
     * Return category for single title
     * title is converted to lower case to make search case-insensitive
     * @param title
     * @return
     */
    private fun getTitleCategories(title: String): Observable<List<String>> {
        return categoryClient.searchCategories(title.toLowerCase(), SEARCH_CATS_LIMIT)
    }


    /**
     * Handles category item selection
     * @param item
     */
    fun onCategoryItemClicked(item: CategoryItem) {
        if (item.isSelected) {
            selectedCategories.add(item)
            updateCategoryCount(item)
        } else {
            selectedCategories.remove(item)
        }
    }

    /**
     * Get Selected Categories
     * @return
     */
    fun getSelectedCategories(): List<CategoryItem> {
        return selectedCategories
    }

    /**
     * Cleanup the existing in memory cache's
     */
    fun cleanUp() {
        selectedCategories.clear()
    }

    companion object {
        private const val SEARCH_CATS_LIMIT = 25
    }
}
