package fr.free.nrw.commons.upload;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

/**
 * Retrofit calls for managing responses network calls of entity ids required for uploading depictions
 */

public interface WikiBaseInterface {

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=wbeditentity")
    Observable<MwPostResponse> postEditEntity(@NonNull @Field("id") String fileEntityId,
                                              @NonNull @Field("token") String editToken,
                                              @NonNull @Field("data") String data);

    @GET(MW_API_PREFIX + "action=query&prop=info")
    Observable<MwQueryResponse> getFileEntityId(@Query("titles") String fileName);

}