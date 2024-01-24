package fr.free.nrw.commons.auth.csrf

import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers

interface CsrfTokenInterface {
    @Headers("Cache-Control: no-cache")
    @GET(Service.MW_API_PREFIX + "action=query&meta=tokens&type=csrf")
    fun getCsrfTokenCall(): Call<MwQueryResponse?>
}
