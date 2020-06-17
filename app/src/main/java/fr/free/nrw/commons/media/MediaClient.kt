package fr.free.nrw.commons.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.Depictions.Companion.from
import fr.free.nrw.commons.utils.CommonsDateUtil
import io.reactivex.Observable
import io.reactivex.Single
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.wikidata.Entities
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

/**
 * Media Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
class MediaClient @Inject constructor(
    private val mediaInterface: MediaInterface,
    private val pageMediaInterface: PageMediaInterface,
    private val mediaDetailInterface: MediaDetailInterface
) {

    //OkHttpJsonApiClient used JsonKvStore for this. I don't know why.
    private val continuationStore: MutableMap<String, Map<String, String>?>
    private val continuationExists: MutableMap<String, Boolean>

    /**
     * Checks if a page exists on Commons
     * The same method can be used to check for file or talk page
     *
     * @param title File:Test.jpg or Commons:Deletion_requests/File:Test1.jpeg
     */
    fun checkPageExistsUsingTitle(title: String?): Single<Boolean> {
        return mediaInterface.checkPageExistsUsingTitle(title)
            .map { mwQueryResponse: MwQueryResponse ->
                mwQueryResponse
                    .query()!!.firstPage()!!.pageId() > 0
            }
            .singleOrError()
    }

    /**
     * Take the fileSha and returns whether a file with a matching SHA exists or not
     *
     * @param fileSha SHA of the file to be checked
     */
    fun checkFileExistsUsingSha(fileSha: String?): Single<Boolean> {
        return mediaInterface.checkFileExistsUsingSha(fileSha)
            .map { mwQueryResponse: MwQueryResponse ->
                mwQueryResponse
                    .query()!!.allImages().size > 0
            }
            .singleOrError()
    }

    /**
     * This method takes the category as input and returns a list of  Media objects filtered using image generator query
     * It uses the generator query API to get the images searched using a query, 10 at a time.
     *
     * @param category the search category. Must start with "Category:"
     * @return
     */
    fun getMediaListFromCategory(category: String): Single<List<Media>> {
        return responseToMediaList(
            if (continuationStore.containsKey("category_$category")) mediaInterface.getMediaListFromCategory(
                category,
                10,
                continuationStore["category_$category"]
            ) else  //if true
                mediaInterface.getMediaListFromCategory(
                    category,
                    10,
                    emptyMap()
                ),
            "category_$category"
        ) //if false
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
        val continuation =
            if (continuationStore.containsKey("user_$userName")) continuationStore["user_$userName"] else emptyMap()
        return responseToMediaList(
            mediaInterface
                .getMediaListForUser(userName, 10, continuation), "user_$userName"
        )
    }

    /**
     * Check if media for user has reached the end of the list.
     * @param userName
     * @return
     */
    fun doesMediaListForUserHaveMorePages(userName: String): Boolean {
        val key = "user_$userName"
        return if (continuationExists.containsKey(key)) {
            continuationExists[key]!!
        } else true
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
    fun getMediaListFromSearch(
        keyword: String?,
        limit: Int,
        offset: Int
    ): Single<MwQueryResponse> {
        return mediaInterface.getMediaListFromSearch(keyword, limit, offset)
    }

    private fun responseToMediaList(
        response: Observable<MwQueryResponse>,
        key: String
    ): Single<List<Media>> {
        return response.flatMap { mwQueryResponse: MwQueryResponse? ->
            if (null == mwQueryResponse || null == mwQueryResponse.query() || null == mwQueryResponse.query()!!
                    .pages()
            ) {
                return@flatMap Observable.empty<MwQueryPage>()
            }
            if (mwQueryResponse.continuation() != null) {
                continuationStore[key] = mwQueryResponse.continuation()
                continuationExists[key] = true
            } else {
                continuationExists[key] = false
            }
            Observable.fromIterable(mwQueryResponse.query()!!.pages())
        }
            .map { page: MwQueryPage? -> Media.from(page) }
            .collect(
                { ArrayList() }
            ) { obj: MutableList<Media>, e: Media ->
                obj.add(
                    e
                )
            }.map { it.toList() }
    }

    /**
     * Fetches Media object from the imageInfo API
     *
     * @param titles the tiles to be searched for. Can be filename or template name
     * @return
     */
    fun getMedia(titles: String?): Single<Media> {
        return mediaInterface.getMedia(titles)
            .flatMap { mwQueryResponse: MwQueryResponse? ->
                if (null == mwQueryResponse || null == mwQueryResponse.query() || null == mwQueryResponse.query()!!
                        .firstPage()
                ) {
                    return@flatMap Observable.empty<MwQueryPage>()
                }
                Observable.just(mwQueryResponse.query()!!.firstPage())
            }
            .map { page: MwQueryPage? -> Media.from(page) }
            .single(Media.EMPTY)
    }

    /**
     * The method returns the picture of the day
     *
     * @return Media object corresponding to the picture of the day
     */
    val pictureOfTheDay: Single<Media>
        get() {
            val date =
                CommonsDateUtil.getIso8601DateFormatShort().format(Date())
            Timber.d("Current date is %s", date)
            val template = "Template:Potd/$date"
            return mediaInterface.getMediaWithGenerator(template)
                .flatMap { mwQueryResponse: MwQueryResponse? ->
                    if (null == mwQueryResponse || null == mwQueryResponse.query() || null == mwQueryResponse.query()!!
                            .firstPage()
                    ) {
                        return@flatMap Observable.empty<MwQueryPage>()
                    }
                    Observable.just(mwQueryResponse.query()!!.firstPage())
                }
                .map { page: MwQueryPage? -> Media.from(page) }
                .single(Media.EMPTY)
        }

    fun getPageHtml(title: String?): Single<String> {
        return mediaInterface.getPageHtml(title)
            .filter { obj: MwParseResponse -> obj.success() }
            .map { obj: MwParseResponse -> obj.parse() }
            .map { obj: MwParseResult? -> obj!!.text() }
            .first("")
    }

    /**
     * @return  caption for image using wikibaseIdentifier
     */
    fun getCaptionByWikibaseIdentifier(wikibaseIdentifier: String?): Single<String> {
        return mediaDetailInterface.getEntityForImage(
            Locale.getDefault().language,
            wikibaseIdentifier
        )
            .map { mediaDetailResponse: Entities ->
                if (isSuccess(mediaDetailResponse)) {
                    for (wikibaseItem in mediaDetailResponse.entities().values) {
                        for (label in wikibaseItem.labels().values) {
                            return@map label.value()
                        }
                    }
                }
                NO_CAPTION
            }
            .singleOrError()
    }

    fun doesPageContainMedia(title: String?): Single<Boolean> {
        return pageMediaInterface.getMediaList(title)
            .map { it.items.isNotEmpty() }
    }

    private fun isSuccess(response: Entities?): Boolean {
        return response != null && response.success == 1 && response.entities() != null
    }

    /**
     * Fetches Structured data from API
     *
     * @param filename
     * @return a map containing caption and depictions (empty string in the map if no caption/depictions)
     */
    fun getDepictions(filename: String?): Single<Depictions> {
        return mediaDetailInterface.fetchEntitiesByFileName(
            Locale.getDefault().language, filename
        )
            .map { entities: Entities? ->
                from(
                    entities!!,
                    this
                )
            }
            .singleOrError()
    }

    /**
     * Gets labels for Depictions using Entity Id from MediaWikiAPI
     *
     * @param entityId  EntityId (Ex: Q81566) of the depict entity
     * @return label
     */
    fun getLabelForDepiction(
        entityId: String?,
        language: String
    ): Single<String> {
        return mediaDetailInterface.getEntity(entityId)
            .map { entities: Entities ->
                if (isSuccess(entities)) {
                    for (entity in entities.entities().values) {
                        val languageToLabelMap =
                            entity.labels()
                        if (languageToLabelMap.containsKey(language)) {
                            return@map languageToLabelMap[language]!!.value()
                        }
                        for (label in languageToLabelMap.values) {
                            return@map label.value()
                        }
                    }
                }
                throw RuntimeException("failed getEntities")
            }
    }

    fun getEntities(entityId: String?): Single<Entities> {
        return mediaDetailInterface.getEntity(entityId)
    }

    companion object {
        const val NO_CAPTION = "No caption"
        private const val NO_DEPICTION = "No depiction"
    }

    init {
        continuationStore =
            HashMap()
        continuationExists = HashMap()
    }
}
