package fr.free.nrw.commons.review;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface class for peer review calls
 */
public interface ReviewInterface {
    @GET("w/api.php?action=query&format=json&list=recentchanges&continue=-||&formatversion=2&rcnamespace=6&rcprop=title|ids&rclimit=20&rctype=new|log&rctoponly=1")
    Observable<MwQueryResponse> getRecentChanges();

    @GET("w/api.php?action=query&format=json&formatversion=2&prop=revisions&rvprop=timestamp|ids|user&rvdir=newer&rvlimit=1")
    Observable<MwQueryResponse> getFirstRevisionOfFile(@Query("titles") String titles);
}
