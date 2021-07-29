package fr.free.nrw.commons.category

import io.reactivex.Single
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Category Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
class ExtendedCategoryClient
@Inject constructor(private val extendedCategoryInterface : ExtendedCategoryInterface) :
    ContinuationClient<MwQueryResponse, String>() {

    fun getCategoryThumbnail(category: String):
            Single<MwQueryPage?> {
        return extendedCategoryInterface.getCategoryThumbnail(category).map {
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
