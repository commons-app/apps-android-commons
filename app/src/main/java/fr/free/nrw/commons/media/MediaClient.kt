package fr.free.nrw.commons.media

import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.media.MediaConverter
import fr.free.nrw.commons.utils.CommonsDateUtil
import io.reactivex.Single
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.wikidata.Entities
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

const val PAGE_ID_PREFIX = "M"
const val CATEGORY_CONTINUATION_PREFIX = "category_"

/**
 * Media Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
class MediaClient @Inject constructor(
    private val mediaInterface: MediaInterface,
    private val pageMediaInterface: PageMediaInterface,
    private val mediaDetailInterface: MediaDetailInterface,
    private val mediaConverter: MediaConverter
) {

    fun getMediaById(id: String) =
        responseToMediaList(mediaInterface.getMediaById(id)).map { it.first() }

    //OkHttpJsonApiClient used JsonKvStore for this. I don't know why.
    private val continuationStore: MutableMap<String, Map<String, String>?> = mutableMapOf()
    private val continuationExists: MutableMap<String, Boolean> = mutableMapOf()

    /**
     * Checks if a page exists on Commons
     * The same method can be used to check for file or talk page
     *
     * @param title File:Test.jpg or Commons:Deletion_requests/File:Test1.jpeg
     */
    fun checkPageExistsUsingTitle(title: String?): Single<Boolean> {
        return mediaInterface.checkPageExistsUsingTitle(title)
            .map { it.query()!!.firstPage()!!.pageId() > 0 }
    }

    /**
     * Take the fileSha and returns whether a file with a matching SHA exists or not
     *
     * @param fileSha SHA of the file to be checked
     */
    fun checkFileExistsUsingSha(fileSha: String?): Single<Boolean> {
        return mediaInterface.checkFileExistsUsingSha(fileSha)
            .map { it.query()!!.allImages().size > 0 }
    }

    /**
     * This method takes the category as input and returns a list of  Media objects filtered using image generator query
     * It uses the generator query API to get the images searched using a query, 10 at a time.
     *
     * @param category the search category. Must start with "Category:"
     * @return
     */
    fun getMediaListFromCategory(category: String): Single<List<Media>> {
        val key = "$CATEGORY_CONTINUATION_PREFIX$category"
        return if (hasMorePagesFor(key)) {
            responseToMediaList(
                mediaInterface.getMediaListFromCategory(
                    category,
                    10,
                    continuationStore[key] ?: emptyMap()
                ),
                key
            )
        } else {
            Single.just(emptyList())
        }
    }

    /**
     * This method takes the userName as input and returns a list of  Media objects filtered using
     * allimages query It uses the allimages query API to get the images contributed by the userName,
     * 10 at a time.
     *
     * @param userName the username
     * @return
     */
    fun getMediaListForUser(userName: String): Single<List<Media>> {
        return responseToMediaList(
            mediaInterface.getMediaListForUser(
                userName,
                10,
                continuationStore["user_$userName"] ?: Collections.emptyMap()
            ),
            "user_$userName"
        )
    }


    /**
     * This method takes a keyword as input and returns a list of  Media objects filtered using image generator query
     * It uses the generator query API to get the images searched using a query, 10 at a time.
     *
     * @param keyword the search keyword
     * @param limit
     * @param offset
     * @return
     */
    fun getMediaListFromSearch(keyword: String?, limit: Int, offset: Int) =
        responseToMediaList(mediaInterface.getMediaListFromSearch(keyword, limit, offset))

    /**
     * @return list of images for a particular depict entity
     */
    fun fetchImagesForDepictedItem(
        query: String,
        srlimit: Int,
        sroffset: Int
    ): Single<List<Media>> {
        return responseToMediaList(
            mediaInterface.fetchImagesForDepictedItem(
                "haswbstatement:" + BuildConfig.DEPICTS_PROPERTY + "=" + query,
                srlimit.toString(),
                sroffset.toString()
            )
        )
    }

    private fun responseToMediaList(
        response: Single<MwQueryResponse>,
        key: String? = null
    ): Single<List<Media>> {
        return response.map {
            if (key != null) {
                continuationExists[key] =
                    it.continuation()?.let { continuation ->
                        continuationStore[key] = continuation
                        true
                    } ?: false
            }
            it.query()?.pages() ?: emptyList()
        }.flatMap(::mediaFromPageAndEntity)

    }

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
     * Fetches Media object from the imageInfo API
     *
     * @param titles the tiles to be searched for. Can be filename or template name
     * @return
     */
    fun getMedia(titles: String?): Single<Media> {
        return responseToMediaList(mediaInterface.getMedia(titles))
            .map { it.first() }
    }

    /**
     * The method returns the picture of the day
     *
     * @return Media object corresponding to the picture of the day
     */
    fun getPictureOfTheDay(): Single<Media> {
        val date = CommonsDateUtil.getIso8601DateFormatShort().format(Date())
        return responseToMediaList(mediaInterface.getMediaWithGenerator("Template:Potd/$date")).map { it.first() }

    }

    fun getPageHtml(title: String?): Single<String> {
        return mediaInterface.getPageHtml(title)
            .map { obj: MwParseResponse -> obj.parse()?.text() ?: "" }
    }

    fun getEntities(entityIds: List<String>): Single<Entities> {
        return if (entityIds.isEmpty())
            Single.error(Exception("empty list passed for ids"))
        else
            mediaDetailInterface.getEntity(entityIds.joinToString("|"))
    }


    /**
     * Check if media for user has reached the end of the list.
     * @param userName
     * @return
     */
    fun doesMediaListForUserHaveMorePages(userName: String): Boolean {
        return hasMorePagesFor("user_$userName")
    }

    private fun hasMorePagesFor(key: String) = continuationExists[key] ?: true

    fun doesPageContainMedia(title: String?): Single<Boolean> {
        return pageMediaInterface.getMediaList(title)
            .map { it.items.isNotEmpty() }
    }

    fun resetCategoryContinuation(category: String) {
        continuationExists.remove("$CATEGORY_CONTINUATION_PREFIX$category")
        continuationStore.remove("$CATEGORY_CONTINUATION_PREFIX$category")
    }
}
