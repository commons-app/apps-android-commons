package fr.free.nrw.commons.upload

import fr.free.nrw.commons.upload.depicts.Claims
import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit calls for managing responses network calls of entity ids required for uploading depictions
 */
interface WikiBaseInterface {
    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(WikidataConstants.MW_API_PREFIX + "action=wbeditentity")
    fun postEditEntity(
        @Field("id") fileEntityId: String,
        @Field("token") editToken: String,
        @Field("data") data: String
    ): Observable<MwPostResponse>

    /**
     * Uploads depicts for a file in the server
     *
     * @param filename name of the file
     * @param editToken editToken for the file
     * @param data data of the depicts to be uploaded
     * @return Observable<MwPostResponse>
    </MwPostResponse> */
    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(WikidataConstants.MW_API_PREFIX + "action=wbeditentity&site=commonswiki")
    fun postEditEntityByFilename(
        @Field("title") filename: String,
        @Field("token") editToken: String,
        @Field("data") data: String
    ): Observable<MwPostResponse>

    @GET(WikidataConstants.MW_API_PREFIX + "action=query&prop=info")
    fun getFileEntityId(@Query("titles") fileName: String?): Observable<MwQueryResponse>

    /**
     * Upload Captions for the image when upload is successful
     *
     * @param fileEntityId enityId for the uploaded file
     * @param editToken editToken for the file
     * @param captionValue value of the caption to be uploaded
     */
    @FormUrlEncoded
    @POST(WikidataConstants.MW_API_PREFIX + "action=wbsetlabel")
    fun addLabelstoWikidata(
        @Field("id") fileEntityId: String?,
        @Field("token") editToken: String?,
        @Field("language") language: String?,
        @Field("value") captionValue: String?
    ): Observable<MwPostResponse>

    @GET(WikidataConstants.MW_API_PREFIX + "action=wbgetclaims")
    fun getClaimsByProperty(
        @Query("entity") entityId: String,
        @Query("property") property: String
    ) : Observable<Claims>

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(WikidataConstants.MW_API_PREFIX + "action=wbeditentity")
    fun postDeleteClaims(
        @Field("token") editToken: String,
        @Field("id") entityId: String,
        @Field("data") data: String
    ): Observable<MwPostResponse>
}
