package fr.free.nrw.commons.wikidata;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import fr.free.nrw.commons.wikidata.model.AddEditTagResponse;
import fr.free.nrw.commons.wikidata.model.WbCreateClaimResponse;
import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

public interface WikidataInterface {

    /**
     * Wikidata create claim API. Posts a new claim for the given entity ID
     */
    @Headers("Cache-Control: no-cache")
    @POST("w/api.php?format=json&errorformat=plaintext&action=wbcreateclaim&errorlang=uselang")
    @Multipart
    Observable<WbCreateClaimResponse> postCreateClaim(@NonNull @Part("entity") RequestBody entity,
                                                      @NonNull @Part("snaktype") RequestBody snakType,
                                                      @NonNull @Part("property") RequestBody property,
                                                      @NonNull @Part("value") RequestBody value,
                                                      @NonNull @Part("uselang") RequestBody useLang,
                                                      @NonNull @Part("token") RequestBody token);

    /**
     * Add edit tag and reason for any revision
     */
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=tag")
    @FormUrlEncoded
    Observable<AddEditTagResponse> addEditTag(@NonNull @Field("revid") String revId,
                                              @NonNull @Field("add") String tagName,
                                              @NonNull @Field("reason") String reason,
                                              @NonNull @Field("token") String token);

    /**
     * Get edit token for wikidata wiki site
     */
    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    @NonNull
    Observable<MwQueryResponse> getCsrfToken();
}
