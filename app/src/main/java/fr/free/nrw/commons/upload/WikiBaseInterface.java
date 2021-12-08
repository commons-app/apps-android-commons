package fr.free.nrw.commons.upload;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

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

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=wbeditentity&site=commonswiki")
    Observable<MwPostResponse> postEditEntityByFilename(@NonNull @Field("title") String fileEntityId,
        @NonNull @Field("token") String editToken,
        @NonNull @Field("data") String data);

    @GET(MW_API_PREFIX + "action=query&prop=info")
    Observable<MwQueryResponse> getFileEntityId(@Query("titles") String fileName);

    /**
     * Upload Captions for the image when upload is successful
     *
     * @param fileEntityId enityId for the uploaded file
     * @param editToken editToken for the file
     * @param captionValue value of the caption to be uploaded
     */
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=wbsetlabel")
    Observable<MwPostResponse> addLabelstoWikidata(@Field("id") String fileEntityId,
        @Field("token") String editToken,
        @Field("language") String language,
        @Field("value") String captionValue);

}
