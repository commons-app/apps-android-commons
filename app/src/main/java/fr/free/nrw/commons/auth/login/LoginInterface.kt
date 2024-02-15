package fr.free.nrw.commons.auth.login

import fr.free.nrw.commons.wikidata.WikidataConstants.MW_API_PREFIX
import io.reactivex.Observable
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface LoginInterface {
    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=login")
    fun getLoginToken(): Call<MwQueryResponse?>

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    fun postLogIn(
        @Field("username") user: String?,
        @Field("password") pass: String?,
        @Field("logintoken") token: String?,
        @Field("uselang") userLanguage: String?,
        @Field("loginreturnurl") url: String?
    ): Call<LoginResponse?>

    @Headers("Cache-Control: no-cache")
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    fun postLogIn(
        @Field("username") user: String?,
        @Field("password") pass: String?,
        @Field("retype") retypedPass: String?,
        @Field("OATHToken") twoFactorCode: String?,
        @Field("logintoken") token: String?,
        @Field("uselang") userLanguage: String?,
        @Field("logincontinue") loginContinue: Boolean
    ): Call<LoginResponse?>

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&list=users&usprop=groups|cancreate")
    fun getUserInfo(@Query("ususers") userName: String): Observable<MwQueryResponse?>
}