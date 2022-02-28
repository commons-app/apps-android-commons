package fr.free.nrw.commons.media

import fr.free.nrw.commons.BetaConstants
import fr.free.nrw.commons.data.models.Media
import fr.free.nrw.commons.category.ContinuationClient
import fr.free.nrw.commons.explore.media.MediaConverter
import io.reactivex.Single
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.wikidata.Entities
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media Client to handle custom calls to Commons MediaWiki APIs of production server
 */
@Singleton
class WikidataMediaClient @Inject constructor(
    private val wikidataMediaInterface: WikidataMediaInterface,
    private val mediaDetailInterface: MediaDetailInterface,
    private val mediaConverter: MediaConverter
) : ContinuationClient<MwQueryResponse, Media>() {

    /**
     * Fetch images for depict ID
     *
     * @param query depictionEntityId ex. "Q9394"
     * @param srlimit the number of items to fetch
     * @param sroffset number of depictions already fetched,
     *                this is useful in implementing pagination
     * @return list of images for a particular depict ID
     */
    fun fetchImagesForDepictedItem(
        query: String,
        srlimit: Int,
        sroffset: Int
    ): Single<List<Media>> {
        return responseMapper(
            wikidataMediaInterface.fetchImagesForDepictedItem(
                "haswbstatement:" + BetaConstants.DEPICTS_PROPERTY + "=" + query,
                srlimit.toString(),
                sroffset.toString()
            )
        )
    }

    /**
     * Helps to map to the required data from the API response
     *
     * @param networkResult MwQueryResponse
     * @param key for handling continuation request, this is null in this case
     */
    override fun responseMapper(
        networkResult: Single<MwQueryResponse>,
        key: String?
    ): Single<List<Media>> {
        return networkResult.map {
            handleContinuationResponse(it.continuation(), key)
            it.query()?.pages() ?: emptyList()
        }.flatMap(::mediaFromPageAndEntity)
    }

    /**
     * Gets list of Media from MwQueryPage
     */
    private fun mediaFromPageAndEntity(pages: List<MwQueryPage>): Single<List<Media>> {
        return if (pages.isEmpty())
            Single.just(emptyList())
        else
            getEntities(pages.map { "$PAGE_ID_PREFIX${it.pageId()}" })
                .map {
                    pages.zip(it.entities().values)
                        .mapNotNull { (page, entity) ->
                            page.imageInfo()?.let {
                                mediaConverter.convert(page, entity, it)
                            }
                        }
                }
    }

    /**
     * Gets Entities from IDs
     *
     * @param entityIds list of IDs of pages/entities ex. {"M4254154", "M11413343"}
     */
    fun getEntities(entityIds: List<String>): Single<Entities> {
        return if (entityIds.isEmpty())
            Single.error(Exception("empty list passed for ids"))
        else
            mediaDetailInterface.getEntity(entityIds.joinToString("|"))
    }

}
