package fr.free.nrw.commons.media;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MediaInterface {
    @GET("w/api.php?action=query")
    Observable<MwQueryResponse> doesPageExist(@Query("titles") String title);

    @GET("w/api.php?action=query&list=allimages")
    Observable<MwQueryResponse> doesFileExist(@Query("aisha1") String aisha1);
}
