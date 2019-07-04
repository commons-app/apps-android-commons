package fr.free.nrw.commons.category;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for interacting with Commons category related APIs
 */
public interface CategoryInterface {

    @GET("w/api.php?action=query&format=json&formatversion=2")
    Observable<MwQueryResponse> searchCategories(@Query("srsearch") String filterValue, @Query("srlimit") int searchCatsLimit);
}
