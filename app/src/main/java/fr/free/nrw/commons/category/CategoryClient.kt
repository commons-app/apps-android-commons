package fr.free.nrw.commons.category

import io.reactivex.Observable
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Category Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
class CategoryClient @Inject constructor(private val categoryInterface: CategoryInterface) {
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
            Observable<List<String>> {
        return responseToCategoryName(categoryInterface.searchCategories(filter, itemLimit, offset))
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
            Observable<List<String>> {
        return responseToCategoryName(
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
    fun getSubCategoryList(categoryName: String?): Observable<List<String>> {
        return responseToCategoryName(categoryInterface.getSubCategoryList(categoryName))
    }

    /**
     * The method takes categoryName as input and returns a List of parent categories
     * It uses the generator query API to get the parent categories of a category, 500 at a time.
     *
     * @param categoryName Category name as defined on commons
     * @return
     */
    fun getParentCategoryList(categoryName: String?): Observable<List<String>> {
        return responseToCategoryName(categoryInterface.getParentCategoryList(categoryName))
    }

    /**
     * Internal function to reduce code reuse. Extracts the categories returned from MwQueryResponse.
     *
     * @param responseObservable The query response observable
     * @return Observable emitting the categories returned. If our search yielded "Category:Test", "Test" is emitted.
     */
    private fun responseToCategoryName(responseObservable: Observable<MwQueryResponse>): Observable<List<String>> {
        return responseObservable
            .map { it.query()?.pages() ?: emptyList() }
            .map {
                it.map { page -> page.title().replace("Category:", "") }
            }
    }

}
