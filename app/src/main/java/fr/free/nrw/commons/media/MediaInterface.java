package fr.free.nrw.commons.media;

import org.jetbrains.annotations.NotNull;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Interface for interacting with Commons media related APIs
 */
public interface MediaInterface {
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
            "&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=640" + //Media property parameters
            "&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal" +
            "|Artist|LicenseShortName|LicenseUrl")
    Observable<MwQueryResponse> getMediaListFromCategory(@Query("gcmtitle") String category, @Query("gcmlimit") int itemLimit, @QueryMap Map<String, String> continuation);

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
            "&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=640" + //Media property parameters
            "&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal" +
            "|Artist|LicenseShortName|LicenseUrl")
    Observable<MwQueryResponse> getMediaListFromSearch(@Query("gsrsearch") String keyword, @Query("gsrlimit") int itemLimit, @QueryMap Map<String, String> continuation);

}
