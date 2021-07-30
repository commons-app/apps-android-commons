package fr.free.nrw.commons.category

import io.reactivex.Single
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Extended Category Client to handle custom calls to Commons APIs
 */
@Singleton
class ExtendedCategoryClient
@Inject constructor(private val extendedCategoryInterface : ExtendedCategoryInterface) :
    ContinuationClient<MwQueryResponse, String>() {

    /**
     * handles getting thumbnail and description from getCategoryInfo API call
     * @param category title
     * @return Single<MwQueryPage?>
     */
    fun getCategoryInfo(category: String):
            Single<MwQueryPage?> {
        return extendedCategoryInterface.getCategoryInfo(category).map {
            it.query()?.pages()?.get(0)
        }
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
                        page ->
                    !page.categoryInfo().isHidden
                }.map { page -> page.title().replace(CATEGORY_PREFIX, "") }
            }
    }
}
