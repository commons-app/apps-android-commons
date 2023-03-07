package fr.free.nrw.commons.review;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface class for peer review calls
 */
public interface ReviewInterface {
    @GET("w/api.php?action=query&format=json&formatversion=2&list=recentchanges&rcprop=title|ids&rctype=new|log&rctoponly=1&rcnamespace=6&rctag=android%20app%20edit")
    Observable<MwQueryResponse> getRecentChanges(@Query("rcstart") String rcStart);

    @GET("w/api.php?action=query&format=json&formatversion=2&prop=revisions&rvprop=timestamp|ids|user&rvdir=newer&rvlimit=1")
    Observable<MwQueryResponse> getFirstRevisionOfFile(@Query("titles") String titles);

    @GET("w/api.php?action=query&format=json&formatversion=2&prop=fileusage|globalusage")
    Observable<MwQueryResponse> getGlobalUsageInfo(@Query("titles") String title);
}
