package fr.free.nrw.commons.review;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface class for peer review calls
 */
public interface ReviewInterface {

    /**
     * Fetch recent changes from MediaWiki API
     * Calls the API for the latest 50 changes (the default limit is 10)
     * More data can be fetched beyond this limit as the API call includes a continuation field
     * However, since it takes longer to check the review status from the database and display the images
     * as they get repeated before more data is fetched in the background
     * the limit is increased from 10 to 50 using gcmlimit
     *
     */
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=categorymembers&gcmtype=file&gcmsort=timestamp&gcmdir=desc&gcmtitle=Category:Uploaded_with_Mobile/Android&gcmlimit=50")
    Observable<MwQueryResponse> getRecentChanges();

    @GET("w/api.php?action=query&format=json&formatversion=2&prop=revisions&rvprop=timestamp|ids|user&rvdir=newer&rvlimit=1")
    Observable<MwQueryResponse> getFirstRevisionOfFile(@Query("titles") String titles);

    @GET("w/api.php?action=query&format=json&formatversion=2&prop=fileusage|globalusage")
    Observable<MwQueryResponse> getGlobalUsageInfo(@Query("titles") String title);
}
