package fr.free.nrw.commons.upload.depicts;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface DepictsInterface {

    @GET("/w/api.php?action=wbsearchentities&format=json&type=item&language=en&uselang=en")
    Observable<MwQueryResponse> searchForDepicts(@Query("search") String query, @Query("limit") String limit);
}
