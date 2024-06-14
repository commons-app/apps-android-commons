package fr.free.nrw.commons.auth.login

import fr.free.nrw.commons.wikidata.WikidataConstants.MW_API_PREFIX
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface LoginInterface {
    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=login")
    suspend fun getLoginToken(): Response<MwQueryResponse?>

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    suspend fun postLogIn(
        @Field("username") user: String?,
        @Field("password") pass: String?,
        @Field("logintoken") token: String?,
        @Field("uselang") userLanguage: String?,
        @Field("loginreturnurl") url: String?
    ): Response<LoginResponse?>

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    suspend fun postLogIn(
        @Field("username") user: String?,
        @Field("password") pass: String?,
        @Field("retype") retypedPass: String?,
        @Field("OATHToken") twoFactorCode: String?,
        @Field("logintoken") token: String?,
        @Field("uselang") userLanguage: String?,
        @Field("logincontinue") loginContinue: Boolean
    ): Response<LoginResponse?>

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&list=users&usprop=groups|cancreate")
    suspend fun getUserInfo(@Query("ususers") userName: String): Response<MwQueryResponse?>
}