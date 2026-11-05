package fr.free.nrw.commons.media

import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.ContinuationClient
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

private const val COLLECTION_PAGE_SIZE = 10

const val CATEGORY_CONTINUATION_PREFIX = "category_"
const val LIMIT = 30
const val RADIUS = 10000

@Singleton
class MediaCollectionClient
    @Inject
    constructor(
        private val mediaInterface: MediaInterface,
        private val mediaResponseMapper: MediaResponseMapper,
    ) : ContinuationClient<MwQueryResponse, Media>() {
        /**
         * This method takes the category as input and returns a list of Media objects filtered using image generator
         * query
         * It uses the generator query API to get the images searched using a query, 10 at a time.
         *
         * @param category the search category. Must start with "Category:"
         * @return
         */
        fun getMediaListFromCategory(category: String): Single<List<Media>> =
            continuationRequest(CATEGORY_CONTINUATION_PREFIX, category) {
                mediaInterface.getMediaListFromCategory(category, COLLECTION_PAGE_SIZE, it)
            }

        /**
         * This method takes the userName as input and returns a list of  Media objects filtered using
         * allimages query It uses the allimages query API to get the images contributed by the userName,
         * 10 at a time.
         *
         * @param userName the username
         * @return
         */
        fun getMediaListForUser(userName: String): Single<List<Media>> =
            continuationRequest("user_", userName) {
                mediaInterface.getMediaListForUser(userName, COLLECTION_PAGE_SIZE, it)
            }

        /**
         * This method takes a keyword as input and returns a list of Media objects filtered using image generator
         * query
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
            offset: Int,
        ) = responseMapper(mediaInterface.getMediaListFromSearch(keyword, limit, offset))

        /**
         * This method takes coordinate as input and returns a list of  Media objects.
         * It uses the generator query API to get the images searched using a query.
         *
         * @param coordinate coordinate
         * @return
         */
        fun getMediaListFromGeoSearch(coordinate: String?) =
            responseMapper(mediaInterface.getMediaListFromGeoSearch(coordinate, LIMIT, RADIUS))

        /**
         * @return list of images for a particular depict entity
         */
        fun fetchImagesForDepictedItem(
            query: String,
            srlimit: Int,
            sroffset: Int,
        ): Single<List<Media>> =
            responseMapper(
                mediaInterface.fetchImagesForDepictedItem(
                    "haswbstatement:" + BuildConfig.DEPICTS_PROPERTY + "=" + query,
                    srlimit.toString(),
                    sroffset.toString(),
                ),
            )

        fun resetCategoryContinuation(category: String) {
            resetContinuation(CATEGORY_CONTINUATION_PREFIX, category)
        }

        /**
         * Call the resetUserContinuation method
         *
         * @param userName the username
         */
        fun resetUserNameContinuation(userName: String) = resetUserContinuation("user_", userName)

        override fun responseMapper(
            networkResult: Single<MwQueryResponse>,
            key: String?,
        ): Single<List<Media>> =
            mediaResponseMapper.map(
                networkResult.doOnSuccess { handleContinuationResponse(it.continuation(), key) },
            )
    }
