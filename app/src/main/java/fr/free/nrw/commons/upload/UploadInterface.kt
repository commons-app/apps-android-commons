package fr.free.nrw.commons.upload

import com.google.gson.JsonObject
import fr.free.nrw.commons.wikidata.WikidataConstants
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.MultipartBody.Part as MultipartBodyPart

interface UploadInterface {
    @Multipart
    @POST(WikidataConstants.MW_API_PREFIX + "action=upload&stash=1&ignorewarnings=1")
    fun uploadFileToStash(
        @Part("filename") filename: RequestBody?,
        @Part("filesize") totalFileSize: RequestBody?,
        @Part("offset") offset: RequestBody?,
        @Part("filekey") fileKey: RequestBody?,
        @Part("token") token: RequestBody?,
        @Part filePart: MultipartBodyPart
    ): Observable<UploadResponse>

    @Headers("Cache-Control: no-cache")
    @POST(WikidataConstants.MW_API_PREFIX + "action=upload&ignorewarnings=1")
    @FormUrlEncoded
    fun uploadFileFromStash(
        @Field("token") token: String,
        @Field("text") text: String,
        @Field("comment") comment: String,
        @Field("filename") filename: String,
        @Field("filekey") filekey: String
    ): Observable<JsonObject>
}
