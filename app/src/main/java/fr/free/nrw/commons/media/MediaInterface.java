package fr.free.nrw.commons.media;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for interacting with Commons media related APIs
 */
public interface MediaInterface {
    /**
     * Checks if a page exists or not.
     * @param title the title of the page to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2")
    Observable<MwQueryResponse> checkPageExistsUsingTitle(@Query("titles") String title);

    /**
     * Check if file exists
     * @param aisha1 the SHA of the media file to be checked
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&list=allimages")
    Observable<MwQueryResponse> checkFileExistsUsingSha(@Query("aisha1") String aisha1);
}
