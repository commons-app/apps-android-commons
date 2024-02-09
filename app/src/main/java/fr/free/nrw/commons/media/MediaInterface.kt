package fr.free.nrw.commons.media

import fr.free.nrw.commons.OkHttpConnectionFactory.UnsuccessfulResponseInterceptor.SUPPRESS_ERROR_LOG_HEADER
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Interface for interacting with Commons media related APIs
 */
interface MediaInterface {
    /**
     * Checks if a page exists or not.
     *
     * @param title the title of the page to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2")
    fun checkPageExistsUsingTitle(@Query("titles") title: String?): Single<MwQueryResponse>

    /**
     * Check if file exists
     *
     * @param aisha1 the SHA of the media file to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&list=allimages")
    fun checkFileExistsUsingSha(@Query("aisha1") aisha1: String?): Single<MwQueryResponse>

    /**
     * This method retrieves a list of Media objects filtered using image generator query
     *
     * @param category     the category name. Must start with "Category:"
     * @param itemLimit    how many images are returned
     * @param continuation the continuation string from the previous query or empty map
     * @return
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2" +                                  //Basic parameters
        "&generator=categorymembers&gcmtype=file&gcmsort=timestamp&gcmdir=desc$MEDIA_PARAMS"    //Category parameters
    )
    fun getMediaListFromCategory(
        @Query("gcmtitle") category: String?,
        @Query("gcmlimit") itemLimit: Int,
        @QueryMap continuation: Map<String, String>
    ): Single<MwQueryResponse>

    /**
     * This method retrieves a list of Media objects for a given user name
     *
     * @param username     user's Wikimedia Commons username.
     * @param itemLimit    how many images are returned
     * @param continuation the continuation string from the previous query or empty map
     * @return
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2" +  //Basic parameters
        "&generator=allimages&gaisort=timestamp&gaidir=older$MEDIA_PARAMS"
    )
    fun getMediaListForUser(
        @Query("gaiuser") username: String?,
        @Query("gailimit") itemLimit: Int,
        @QueryMap(encoded = true) continuation: Map<String, String>
    ): Single<MwQueryResponse>

    /**
     * This method retrieves a list of Media objects filtered using image generator query
     *
     * @param keyword      the searched keyword
     * @param itemLimit    how many images are returned
     * @param offset       the offset in the result set
     * @return
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2" +          //Basic parameters
        "&generator=search&gsrwhat=text&gsrnamespace=6$MEDIA_PARAMS"    //Search parameters
    )
    fun getMediaListFromSearch(
        @Query("gsrsearch") keyword: String?,
        @Query("gsrlimit") itemLimit: Int, @Query("gsroffset") offset: Int
    ): Single<MwQueryResponse>

    /**
     * This method retrieves a list of Media objects filtered using list geosearch query. Example: https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2&generator=geosearch&ggsnamespace=6&prop=imageinfo|coordinates&iiprop=url|extmetadata|user&&iiurlwidth=640&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl&ggscoord=37.45579%7C-122.31369&ggslimit=30&ggsradius=10000
     *
     * @param location     the search location
     * @param itemLimit    how many images are returned
     * @return
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2" +      //Basic parameters
        "&generator=geosearch&ggsnamespace=6$MEDIA_PARAMS"          //Search parameters
    )
    fun getMediaListFromGeoSearch(
        @Query("ggscoord") location: String?,
        @Query("ggslimit") itemLimit: Int,
        @Query("ggsradius") radius: Int
    ): Single<MwQueryResponse>

    /**
     * Fetches Media object from the imageInfo API
     *
     * @param title       the tiles to be searched for. Can be filename or template name
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2$MEDIA_PARAMS_WITH_CATEGORY_DETAILS")
    fun getMedia(@Query("titles") title: String?): Single<MwQueryResponse>

    /**
     * Fetches Media object from the imageInfo API but suppress (known) errors
     *
     * @param title       the tiles to be searched for. Can be filename or template name
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2$MEDIA_PARAMS_WITH_CATEGORY_DETAILS")
    @Headers(SUPPRESS_ERROR_LOG_HEADER)
    fun getMediaSuppressingErrors(@Query("titles") title: String?): Single<MwQueryResponse>

    /**
     * Fetches Media object from the imageInfo API
     *
     * @param pageIds       the ids to be searched for
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2$MEDIA_PARAMS")
    @Headers(SUPPRESS_ERROR_LOG_HEADER)
    fun getMediaById(@Query("pageids") pageIds: String?): Single<MwQueryResponse>

    /**
     * Fetches Media object from the imageInfo API
     * Passes an image generator parameter
     *
     * @param title       the tiles to be searched for. Can be filename or template name
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=images$MEDIA_PARAMS")
    fun getMediaWithGenerator(@Query("titles") title: String?): Single<MwQueryResponse>

    @GET("w/api.php?format=json&action=parse&prop=text")
    @Headers(SUPPRESS_ERROR_LOG_HEADER)
    fun getPageHtml(@Query("page") title: String?): Single<MwParseResponse>

    /**
     * Fetches caption using file name
     *
     * @param filename name of the file to be used for fetching captions
     */
    @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1")
    fun fetchCaptionByFilename(
        @Query("language") language: String?,
        @Query("titles") filename: String?
    ): Single<MwQueryResponse>

    /**
     * Fetches list of images from a depiction entity
     * @param query depictionEntityId
     * @param srlimit the number of items to fetch
     * @param sroffset number od depictions already fetched, this is useful in implementing pagination
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2" +  //Basic parameters
        "&generator=search&gsrnamespace=6$MEDIA_PARAMS"         //Search parameters
    )
    fun fetchImagesForDepictedItem(
        @Query("gsrsearch") query: String?,
        @Query("gsrlimit") srlimit: String?, @Query("gsroffset") sroffset: String?
    ): Single<MwQueryResponse>

    companion object {
        const val MEDIA_PARAMS =
            "&prop=imageinfo|coordinates&iiprop=url|extmetadata|user&&iiurlwidth=640&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl"

        /**
         * fetches category detail(title, hidden) for each category along with File information
         */
        const val MEDIA_PARAMS_WITH_CATEGORY_DETAILS =
            "&clprop=hidden&prop=categories|imageinfo&iiprop=url|extmetadata|user&&iiurlwidth=640&iiextmetadatafilter=DateTime|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl"
    }
}
