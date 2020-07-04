package fr.free.nrw.commons.category

import io.reactivex.Single
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import javax.inject.Inject
import javax.inject.Singleton

const val CATEGORY_PREFIX = "Category:"
const val SUB_CATEGORY_CONTINUATION_PREFIX = "sub_category_"
const val PARENT_CATEGORY_CONTINUATION_PREFIX = "parent_category_"

/**
 * Category Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
class CategoryClient @Inject constructor(private val categoryInterface: CategoryInterface) :
    ContinuationClient<MwQueryResponse, String>() {

    /**
     * Searches for categories containing the specified string.
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @param offset    Starts returning items from the nth result. If offset is 9, the response starts with the 9th item of the search result
     * @return
     */
    @JvmOverloads
    fun searchCategories(filter: String?, itemLimit: Int, offset: Int = 0):
            Single<List<String>> {
        return responseMapper(categoryInterface.searchCategories(filter, itemLimit, offset))
    }

    /**
     * Searches for categories starting with the specified string.
     *
     * @param prefix    The prefix to be searched
     * @param itemLimit How many results are returned
     * @param offset    Starts returning items from the nth result. If offset is 9, the response starts with the 9th item of the search result
     * @return
     */
    @JvmOverloads
    fun searchCategoriesForPrefix(prefix: String?, itemLimit: Int, offset: Int = 0):
            Single<List<String>> {
        return responseMapper(
            categoryInterface.searchCategoriesForPrefix(prefix, itemLimit, offset)
        )
    }

    /**
     * The method takes categoryName as input and returns a List of Subcategories
     * It uses the generator query API to get the subcategories in a category, 500 at a time.
     *
     * @param categoryName Category name as defined on commons
     * @return Observable emitting the categories returned. If our search yielded "Category:Test", "Test" is emitted.
     */
    fun getSubCategoryList(categoryName: String): Single<List<String>> {
        return continuationRequest(SUB_CATEGORY_CONTINUATION_PREFIX, categoryName) {
            categoryInterface.getSubCategoryList(
                categoryName, it
            )
        }
    }

    /**
     * The method takes categoryName as input and returns a List of parent categories
     * It uses the generator query API to get the parent categories of a category, 500 at a time.
     *
     * @param categoryName Category name as defined on commons
     * @return
     */
    fun getParentCategoryList(categoryName: String): Single<List<String>> {
        return continuationRequest(PARENT_CATEGORY_CONTINUATION_PREFIX, categoryName) {
            categoryInterface.getParentCategoryList(categoryName, it)
        }
    }

    fun resetSubCategoryContinuation(category: String) {
        resetContinuation(SUB_CATEGORY_CONTINUATION_PREFIX, category)
    }

    fun resetParentCategoryContinuation(category: String) {
        resetContinuation(PARENT_CATEGORY_CONTINUATION_PREFIX, category)
    }

    override fun responseMapper(
        networkResult: Single<MwQueryResponse>,
        key: String?
    ): Single<List<String>> {
        return networkResult
            .map {
                handleContinuationResponse(it.continuation(), key)
                it.query()?.pages() ?: emptyList()
            }
            .map {
                it.filter {
                    page -> page.categoryInfo() == null || !page.categoryInfo().isHidden
                }.map { page -> page.title().replace(CATEGORY_PREFIX, "") }
            }
    }
}
