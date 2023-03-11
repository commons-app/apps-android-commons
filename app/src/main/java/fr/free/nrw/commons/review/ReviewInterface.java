package fr.free.nrw.commons.review;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface class for peer review calls
 */
public interface ReviewInterface {
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=categorymembers&gcmtype=file&gcmsort=timestamp&gcmdir=desc&gcmtitle=Category:Uploaded_with_Mobile/Android&gcmlimit=50")
    Observable<MwQueryResponse> getRecentChanges();

    @GET("w/api.php?action=query&format=json&formatversion=2&prop=revisions&rvprop=timestamp|ids|user&rvdir=newer&rvlimit=1")
    Observable<MwQueryResponse> getFirstRevisionOfFile(@Query("titles") String titles);
}
