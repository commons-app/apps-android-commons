package fr.free.nrw.commons.media;

import io.reactivex.Single;
import java.util.Map;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Interface for interacting with Commons media related APIs
 */
public interface MediaInterface {
    String MEDIA_PARAMS="&prop=imageinfo&iiprop=url|extmetadata|user&&iiurlwidth=640" +
            "&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal" +
            "|Artist|LicenseShortName|LicenseUrl";

    /**
     * Checks if a page exists or not.
     *
     * @param title the title of the page to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2")
    Single<MwQueryResponse> checkPageExistsUsingTitle(@Query("titles") String title);

    /**
     * Check if file exists
     *
     * @param aisha1 the SHA of the media file to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&list=allimages")
    Single<MwQueryResponse> checkFileExistsUsingSha(@Query("aisha1") String aisha1);

    /**
     * This method retrieves a list of Media objects filtered using image generator query
     *
     * @param category     the category name. Must start with "Category:"
     * @param itemLimit    how many images are returned
     * @param continuation the continuation string from the previous query or empty map
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" + //Basic parameters
            "&generator=categorymembers&gcmtype=file&gcmsort=timestamp&gcmdir=desc" + //Category parameters
            MEDIA_PARAMS)
    Single<MwQueryResponse> getMediaListFromCategory(@Query("gcmtitle") String category, @Query("gcmlimit") int itemLimit, @QueryMap Map<String, String> continuation);


    /**
     * This method retrieves a list of Media objects for a given user name
     *
     * @param username     user's Wikimedia Commons username.
     * @param itemLimit    how many images are returned
     * @param continuation the continuation string from the previous query or empty map
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" + //Basic parameters
        "&generator=allimages&gaisort=timestamp&gaidir=older" + MEDIA_PARAMS)
    Single<MwQueryResponse> getMediaListForUser(@Query("gaiuser") String username,
        @Query("gailimit") int itemLimit, @QueryMap(encoded = true) Map<String, String> continuation);

    /**
     * This method retrieves a list of Media objects filtered using image generator query
     *
     * @param keyword      the searched keyword
     * @param itemLimit    how many images are returned
     * @param offset       the offset in the result set
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" + //Basic parameters
            "&generator=search&gsrwhat=text&gsrnamespace=6" + //Search parameters
            MEDIA_PARAMS)
    Single<MwQueryResponse> getMediaListFromSearch(@Query("gsrsearch") String keyword, @Query("gsrlimit") int itemLimit, @Query("gsroffset") int offset);

    /**
     * Fetches Media object from the imageInfo API
     *
     * @param title       the tiles to be searched for. Can be filename or template name
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" +
            MEDIA_PARAMS)
    Single<MwQueryResponse> getMedia(@Query("titles") String title);

    /**
     * Fetches Media object from the imageInfo API
     *
     * @param pageIds       the ids to be searched for
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" +
            MEDIA_PARAMS)
    Single<MwQueryResponse> getMediaById(@Query("pageids") String pageIds);

    /**
     * Fetches Media object from the imageInfo API
     * Passes an image generator parameter
     *
     * @param title       the tiles to be searched for. Can be filename or template name
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=images" +
            MEDIA_PARAMS)
    Single<MwQueryResponse> getMediaWithGenerator(@Query("titles") String title);

    @GET("w/api.php?format=json&action=parse&prop=text")
    Single<MwParseResponse> getPageHtml(@Query("page") String title);

    /**
     * Fetches caption using file name
     *
     * @param filename name of the file to be used for fetching captions
     * */
    @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1")
    Single<MwQueryResponse> fetchCaptionByFilename(@Query("language") String language, @Query("titles") String filename);

    /**
     * Fetches list of images from a depiction entity
     *  @param query depictionEntityId
     * @param srlimit the number of items to fetch
     * @param sroffset number od depictions already fetched, this is useful in implementing pagination
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" + //Basic parameters
        "&generator=search&gsrnamespace=6" + //Search parameters
        MEDIA_PARAMS)
    Single<MwQueryResponse> fetchImagesForDepictedItem(@Query("gsrsearch") String query,
        @Query("gsrlimit")String srlimit, @Query("gsroffset") String sroffset);

}
