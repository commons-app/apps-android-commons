package fr.free.nrw.commons.wikidata;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import fr.free.nrw.commons.data.models.wikidata.WbCreateClaimResponse;
import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

public interface WikidataInterface {

  /**
   * Get edit token for wikidata wiki site
   */
  @Headers("Cache-Control: no-cache")
  @GET(MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
  @NonNull
  Observable<MwQueryResponse> getCsrfToken();

  /**
   * Wikidata create claim API. Posts a new claim for the given entity ID
   */
  @Headers("Cache-Control: no-cache")
  @POST("w/api.php?format=json&action=wbsetclaim")
  @FormUrlEncoded
  Observable<WbCreateClaimResponse> postSetClaim(@NonNull @Field("claim") String request,
      @NonNull @Field("tags") String tags,
      @NonNull @Field("token") String token);
}
