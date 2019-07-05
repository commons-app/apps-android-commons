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
     * @param title     the category name. Must start with "Category:"
     * @param itemLimit how many images are returned
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=categorymembers" +
            "&gcmtype=file&gcmsort=timestamp&gcmdir=desc&gcmlimit=10" +
            "&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=640" +
            "&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal" +
            "|Artist|LicenseShortName|LicenseUrl")
    Observable<MwQueryResponse> getCategoryMediaList(@Query("gcmtitle") String title, @Query("gcmlimit") int itemLimit);

    /**
     * This method retrieves a list of Media objects filtered using image generator query
     *
     * @param title        the category name. Must start with "Category:"
     * @param itemLimit    how many images are returned
     * @param continuation the continuation string from the previous query
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=categorymembers" +
            "&gcmtype=file&gcmsort=timestamp&gcmdir=desc" +
            "&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=640" +
            "&iiextmetadatafilter=DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal" +
            "|Artist|LicenseShortName|LicenseUrl")
    Observable<MwQueryResponse> getCategoryMediaList(@Query("gcmtitle") String title, @Query("gcmlimit") int itemLimit, @QueryMap Map<String, String> continuation);

}
