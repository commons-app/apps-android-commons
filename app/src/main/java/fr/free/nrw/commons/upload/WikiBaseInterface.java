package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.wikidata.WikidataConstants.MW_API_PREFIX;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse;
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse;
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

    /**
     * Uploads depicts for a file in the server
     *
     * @param filename name of the file
     * @param editToken editToken for the file
     * @param data data of the depicts to be uploaded
     * @return Observable<MwPostResponse>
     */
    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=wbeditentity&site=commonswiki&clear=1")
    Observable<MwPostResponse> postEditEntityByFilename(@NonNull @Field("title") String filename,
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
