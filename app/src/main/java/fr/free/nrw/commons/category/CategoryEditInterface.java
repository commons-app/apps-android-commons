package fr.free.nrw.commons.category;

import io.reactivex.Observable;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CategoryEditInterface {
    @GET("w//api.php?action=query&format=json&prop=revisions&formatversion=2&rvprop=content&rvslots=*")
    Observable<MwQueryResponse> getContentOfFile(@Query("titles") String titles);
}
