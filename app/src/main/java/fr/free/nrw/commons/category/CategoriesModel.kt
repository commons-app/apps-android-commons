package fr.free.nrw.commons.category

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.upload.GpsCategoryModel
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Observable
import java.util.Date
import javax.inject.Inject

/**
 * The model class for categories in upload
 */
class CategoriesModel
    @Inject
    constructor(
        private val categoryClient: CategoryClient,
        private val categoryDao: CategoryDao,
        private val gpsCategoryModel: GpsCategoryModel,
    ) {
        private val categorySuggestions =
            CategorySuggestions(categoryClient, categoryDao, gpsCategoryModel)

        private val selectedCategories: MutableList<CategoryItem> = mutableListOf()

        /**
         * Existing categories which are selected
         */
        private var selectedExistingCategories: MutableList<String> = mutableListOf()

        /**
         * Returns true if an item is considered to be a spammy category which should be ignored
         *
         * @param item a category item that needs to be validated to know if it is spammy or not
         * @return
         */
        fun isSpammyCategory(item: String): Boolean {
            // always skip irrelevant categories such as Media_needing_categories_as_of_16_June_2017(Issue #750)
            val spammyCategory =
                item.matches("(.*)needing(.*)".toRegex()) ||
                    item.matches("(.*)taken on(.*)".toRegex())

            // checks for
            // dd/mm/yyyy or yy
            // yyyy or yy/mm/dd
            // yyyy or yy/mm
            // mm/yyyy or yy
            // for `yy` it is assumed that 20XX is implicit.
            // with separators [., /, -]
            val isIrrelevantCategory =
                item.contains(
                    """\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{2,4}[-/.]\d{1,2}[-/.]\d{1,2}|\d{2,4}[-/.]\d{1,2}|\d{1,2}[-/.]\d{2,4}"""
                        .toRegex(),
                )

            if (spammyCategory) {
                return true
            }

            if (isIrrelevantCategory) {
                return true
            }

            val hasYear = item.matches("(.*\\d{4}.*)".toRegex())
            val validYearsRange = item.matches(".*(20[0-9]{2}).*".toRegex())

            // finally if there's 4 digits year exists in XXXX it should only be in 20XX range.
            return hasYear && !validYearsRange
        }

        /**
         * Updates category count in category dao
         * @param item
         */
        fun updateCategoryCount(item: CategoryItem) {
            var category = categoryDao.find(item.name)

            // Newly used category...
            if (category == null) {
                category =
                    Category(
                        null,
                        item.name,
                        item.description,
                        item.thumbnail,
                        Date(),
                        0,
                    )
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
        fun searchAll(
            term: String,
            imageTitleList: List<String>,
            selectedDepictions: List<DepictedItem>,
        ): Observable<List<CategoryItem>> =
            categorySuggestions
                .suggestionsOrSearch(term, imageTitleList, selectedDepictions)
                .map { categories ->
                    categories.map { category ->
                        CategoryItem(
                            category.name,
                            category.description,
                            category.thumbnail,
                            false,
                        )
                    }
                }

        /**
         * Fetches details of every category by their name, converts them into
         * CategoryItem and returns them in a list.
         *
         * @param categoryNames selected Categories
         * @return List of CategoryItem
         */
        fun getCategoriesByName(categoryNames: List<String>): Observable<MutableList<CategoryItem>>? =
            Observable
                .fromIterable(categoryNames)
                .map { categoryName ->
                    buildCategories(categoryName)
                }.filter { categoryItem ->
                    categoryItem.name != "Hidden"
                }.toList()
                .toObservable()

        /**
         * Fetches the categories and converts them into CategoryItem
         */
        fun buildCategories(categoryName: String): CategoryItem =
            categoryClient
                .getCategoriesByName(
                    categoryName,
                    categoryName,
                    SEARCH_CATS_LIMIT,
                ).map {
                    if (it.isNotEmpty()) {
                        CategoryItem(
                            it[0].name,
                            it[0].description,
                            it[0].thumbnail,
                            it[0].isSelected,
                        )
                    } else {
                        CategoryItem(
                            "Hidden",
                            "Hidden",
                            "hidden",
                            false,
                        )
                    }
                }.blockingGet()

        /**
         * Handles category item selection
         * @param item
         */
        fun onCategoryItemClicked(
            item: CategoryItem,
            media: Media?,
        ) {
            if (media == null) {
                if (item.isSelected) {
                    selectedCategories.add(item)
                    updateCategoryCount(item)
                } else {
                    selectedCategories.remove(item)
                }
            } else {
                if (item.isSelected) {
                    if (media.categories?.contains(item.name) == true) {
                        selectedExistingCategories.add(item.name)
                    } else {
                        selectedCategories.add(item)
                        updateCategoryCount(item)
                    }
                } else {
                    if (media.categories?.contains(item.name) == true) {
                        selectedExistingCategories.remove(item.name)
                        if (!media.categories?.contains(item.name)!!) {
                            val categoriesList: MutableList<String> = ArrayList()
                            categoriesList.add(item.name)
                            categoriesList.addAll(media.categories!!)
                            media.categories = categoriesList
                        }
                    } else {
                        selectedCategories.remove(item)
                    }
                }
            }
        }

        /**
         * Get Selected Categories
         * @return
         */
        fun getSelectedCategories(): List<CategoryItem> = selectedCategories

        /**
         * Cleanup the existing in memory cache's
         */
        fun cleanUp() {
            selectedCategories.clear()
            selectedExistingCategories.clear()
        }

        companion object {
            const val SEARCH_CATS_LIMIT = 25
        }

        /**
         * Provides selected existing categories
         *
         * @return selected existing categories
         */
        fun getSelectedExistingCategories(): List<String> = selectedExistingCategories

        /**
         * Initialize existing categories
         *
         * @param selectedExistingCategories existing categories
         */
        fun setSelectedExistingCategories(selectedExistingCategories: MutableList<String>) {
            this.selectedExistingCategories = selectedExistingCategories
        }
    }
