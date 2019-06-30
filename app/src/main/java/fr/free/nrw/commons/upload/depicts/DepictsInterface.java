package fr.free.nrw.commons.upload.depicts;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface DepictsInterface {

    @FormUrlEncoded
    @POST("w/api.php?action=wbsearchentities&format=json&type=item&language=en&uselang=en")
    Observable<MwQueryResponse> searchForDepicts(@Field("search") String query, @Field("limit") String limit);
}
