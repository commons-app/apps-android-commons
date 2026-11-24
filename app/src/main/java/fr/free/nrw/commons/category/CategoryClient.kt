package fr.free.nrw.commons.category

import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

const val CATEGORY_PREFIX = "Category:"
const val SUB_CATEGORY_CONTINUATION_PREFIX = "sub_category_"
const val PARENT_CATEGORY_CONTINUATION_PREFIX = "parent_category_"
const val PREFIX_SEARCH_CONTINUATION_PREFIX = "prefix_search_"
const val TEXT_SEARCH_CONTINUATION_PREFIX = "text_search_"
const val CATEGORY_UNCATEGORISED = "uncategorised"
const val CATEGORY_NEEDING_CATEGORIES = "needing categories"

/**
 * Category Client to handle custom calls to Commons MediaWiki APIs.
 * TooManyFunctions is suppressed because every function is one category API call or its paging state.
 */
@Suppress("TooManyFunctions")
@Singleton
class CategoryClient
    @Inject
    constructor(
        private val categoryInterface: CategoryInterface,
    ) : ContinuationClient<MwQueryResponse, CategoryItem>() {
        /**
         * Searches for categories containing the specified string.
         *
         * @param filter    The string to be searched
         * @param itemLimit How many results are returned
         * @param offset Starts returning items from the nth result. If offset is 9, the response starts with the 9th
         * item of the search result
         * @return
         */
        @JvmOverloads
        fun searchCategories(
            filter: String?,
            itemLimit: Int,
            offset: Int = 0,
        ): Single<List<CategoryItem>> = responseMapper(categoryInterface.searchCategories(filter, itemLimit, offset))

        /**
         * Searches for categories starting with the specified string. Starts again from the first
         * page, [nextPrefixPage] continues from here.
         *
         * @param prefix    The prefix to be searched
         * @param itemLimit How many results are returned
         * @return
         */
        fun searchCategoriesForPrefix(
            prefix: String,
            itemLimit: Int,
        ): Single<List<CategoryItem>> {
            resetContinuation(PREFIX_SEARCH_CONTINUATION_PREFIX, prefix)
            return nextPrefixPage(prefix, itemLimit)
        }

        /**
         * Next page of [searchCategoriesForPrefix], empty once there are no more pages.
         */
        fun nextPrefixPage(
            prefix: String,
            itemLimit: Int,
        ): Single<List<CategoryItem>> =
            continuationRequest(PREFIX_SEARCH_CONTINUATION_PREFIX, prefix) {
                categoryInterface.searchCategoriesForPrefix(prefix, itemLimit, it)
            }

        /**
         * Whether [nextPrefixPage] can return more results for this prefix
         */
        fun hasMorePrefixPages(prefix: String) = hasMorePagesFor("$PREFIX_SEARCH_CONTINUATION_PREFIX$prefix")

        /**
         * Next page of the full text category search, which ignores case. Hidden categories are
         * dropped. Empty once there are no more pages.
         */
        fun nextTextSearchPage(
            term: String,
            itemLimit: Int,
        ): Single<List<CategoryItem>> =
            continuationRequest(TEXT_SEARCH_CONTINUATION_PREFIX, term) {
                categoryInterface.searchCategoriesWithInfo(term, itemLimit, it)
            }

        /**
         * Whether [nextTextSearchPage] can return more results for this term
         */
        fun hasMoreTextSearchPages(term: String) = hasMorePagesFor("$TEXT_SEARCH_CONTINUATION_PREFIX$term")

        /**
         * Makes [nextTextSearchPage] start again from the first page
         */
        fun resetTextSearch(term: String) {
            resetContinuation(TEXT_SEARCH_CONTINUATION_PREFIX, term)
        }

        /**
         * Fetches categories starting and ending with a specified name.
         *
         * @param startingCategoryName Name of the category to start
         * @param endingCategoryName Name of the category to end
         * @param itemLimit How many categories to return
         * @param offset offset
         * @return MwQueryResponse
         */
        @JvmOverloads
        fun getCategoriesByName(
            startingCategoryName: String?,
            endingCategoryName: String?,
            itemLimit: Int,
            offset: Int = 0,
        ): Single<List<CategoryItem>> =
            responseMapper(
                categoryInterface.getCategoriesByName(
                    startingCategoryName,
                    endingCategoryName,
                    itemLimit,
                    offset,
                ),
            )

        /**
         * Fetches categories belonging to an image (P18 of some wikidata entity).
         *
         * @param image P18 of some wikidata entity
         * @param itemLimit How many categories to return
         * @return Single Observable emitting the list of categories
         */
        fun getCategoriesOfImage(
            image: String,
            itemLimit: Int,
        ): Single<List<CategoryItem>> =
            responseMapper(
                categoryInterface.getCategoriesByTitles(
                    "File:$image",
                    itemLimit,
                ),
            )

        /**
         * The method takes categoryName as input and returns a List of Subcategories
         * It uses the generator query API to get the subcategories in a category, 500 at a time.
         *
         * @param categoryName Category name as defined on commons
         * @return Observable emitting the categories returned. If our search yielded "Category:Test", "Test" is
         * emitted.
         */
        fun getSubCategoryList(categoryName: String): Single<List<CategoryItem>> =
            continuationRequest(SUB_CATEGORY_CONTINUATION_PREFIX, categoryName) {
                categoryInterface.getSubCategoryList(
                    categoryName,
                    it,
                )
            }

        /**
         * The method takes categoryName as input and returns a List of parent categories
         * It uses the generator query API to get the parent categories of a category, 500 at a time.
         *
         * @param categoryName Category name as defined on commons
         * @return
         */
        fun getParentCategoryList(categoryName: String): Single<List<CategoryItem>> =
            continuationRequest(PARENT_CATEGORY_CONTINUATION_PREFIX, categoryName) {
                categoryInterface.getParentCategoryList(categoryName, it)
            }

        fun resetSubCategoryContinuation(category: String) {
            resetContinuation(SUB_CATEGORY_CONTINUATION_PREFIX, category)
        }

        fun resetParentCategoryContinuation(category: String) {
            resetContinuation(PARENT_CATEGORY_CONTINUATION_PREFIX, category)
        }

        override fun responseMapper(
            networkResult: Single<MwQueryResponse>,
            key: String?,
        ): Single<List<CategoryItem>> =
            networkResult
                .map {
                    handleContinuationResponse(it.continuation(), key)
                    it.query()?.pages() ?: emptyList()
                }.map { pages ->
                    // Search results come unordered, their index is the rank
                    val isSearch =
                        key?.startsWith(PREFIX_SEARCH_CONTINUATION_PREFIX) == true ||
                            key?.startsWith(TEXT_SEARCH_CONTINUATION_PREFIX) == true
                    if (isSearch) pages.sortedBy { it.index() } else pages
                }.map {
                    it
                        .filter { page ->
                            // Null check is not redundant because some values could be null
                            // for mocks when running unit tests
                            page.categoryInfo()?.isHidden != true
                        }.map {
                            CategoryItem(
                                it.title().replace(CATEGORY_PREFIX, ""),
                                it.description().toString(),
                                it.thumbUrl().toString(),
                                false,
                            )
                        }
                }
    }
