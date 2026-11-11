package fr.free.nrw.commons.wikidata

import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.WbCreateClaimResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface WikidataInterface {
    /** Reads the current statements and revision before an image edit. */
    @Headers("Cache-Control: no-cache")
    @GET(WikidataConstants.MW_API_PREFIX + "action=wbgetentities&props=info|claims")
    fun getItemForImageEdit(
        @Query("ids") id: String,
    ): Single<Entities>

    /**
     * Get edit token for wikidata wiki site
     */
    @Headers("Cache-Control: no-cache")
    @GET(WikidataConstants.MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    fun getCsrfToken(): Observable<MwQueryResponse>

    /**
     * Wikidata create claim API. Posts a new claim for the given entity ID
     */
    @Headers("Cache-Control: no-cache")
    @POST("w/api.php?format=json&action=wbsetclaim")
    @FormUrlEncoded
    fun postSetClaim(
        @Field("claim") request: String,
        @Field("tags") tags: String,
        @Field("token") token: String,
        @Field("baserevid") baseRevision: Long,
    ): Observable<WbCreateClaimResponse>
}
