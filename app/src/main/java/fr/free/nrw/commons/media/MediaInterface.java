package fr.free.nrw.commons.media;

import io.reactivex.Observable;
import java.util.Map;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Interface for interacting with Commons media related APIs
 */
public interface MediaInterface {
    String MEDIA_PARAMS="&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=640" +
            "&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal" +
            "|Artist|LicenseShortName|LicenseUrl";

    String USER_MEDIA_PARAMS = "&prop=imageinfo&aiprop=url|extmetadata&iiurlwidth=640" +
        "&aiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal"
        +
        "|Artist|LicenseShortName|LicenseUrl";
    /**
     * Checks if a page exists or not.
     *
     * @param title the title of the page to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2")
    Observable<MwQueryResponse> checkPageExistsUsingTitle(@Query("titles") String title);

    /**
     * Check if file exists
     *
     * @param aisha1 the SHA of the media file to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&list=allimages")
    Observable<MwQueryResponse> checkFileExistsUsingSha(@Query("aisha1") String aisha1);

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
    Observable<MwQueryResponse> getMediaListFromCategory(@Query("gcmtitle") String category, @Query("gcmlimit") int itemLimit, @QueryMap Map<String, String> continuation);

    /**
     * This method retrieves a list of Media objects for a given user name
     *
     * @param username     the category name. Must start with "Category:"
     * @param itemLimit    how many images are returned
     * @param continuation the continuation string from the previous query or empty map
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" + //Basic parameters
        "&generator=categorymembers&gcmtype=file&gcmsort=timestamp&gcmdir=desc" +
        //Category parameters
        MEDIA_PARAMS)
    Observable<MwQueryResponse> getMediaListForUser(@Query("aiuser") String username,
        @Query("ailimit") int itemLimit, @QueryMap Map<String, String> continuation);

    /**
     * This method retrieves a list of Media objects filtered using image generator query
     *
     * @param keyword      the searched keyword
     * @param itemLimit    how many images are returned
     * @param continuation the continuation string from the previous query
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" + //Basic parameters
            "&generator=search&gsrwhat=text&gsrnamespace=6" + //Search parameters
            MEDIA_PARAMS)
    Observable<MwQueryResponse> getMediaListFromSearch(@Query("gsrsearch") String keyword, @Query("gsrlimit") int itemLimit, @QueryMap Map<String, String> continuation);

    /**
     * Fetches Media object from the imageInfo API
     *
     * @param title       the tiles to be searched for. Can be filename or template name
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" +
            MEDIA_PARAMS)
    Observable<MwQueryResponse> getMedia(@Query("titles") String title);

    /**
     * Fetches Media object from the imageInfo API
     * Passes an image generator parameter
     *
     * @param title       the tiles to be searched for. Can be filename or template name
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=images" +
            MEDIA_PARAMS)
    Observable<MwQueryResponse> getMediaWithGenerator(@Query("titles") String title);

    @GET("w/api.php?format=json&action=parse&prop=text")
    Observable<MwParseResponse> getPageHtml(@Query("page") String title);
}
