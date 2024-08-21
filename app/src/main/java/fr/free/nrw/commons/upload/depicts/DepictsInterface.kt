package fr.free.nrw.commons.upload.depicts

import fr.free.nrw.commons.wikidata.model.DepictSearchResponse
import fr.free.nrw.commons.wikidata.model.Entities
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Manges retrofit calls for Searching of depicts from DepictsFragment
 */
interface DepictsInterface {
    /**
     * Search for depictions using the wbsearchentities API
     * @param query search for depictions based on user query
     * @param limit number of depictions to be retrieved
     * @param language current locale of the phone
     * @param uselang current locale of the phone
     * @param offset number of depictions already fetched useful in implementing pagination
     */
    @GET("/w/api.php?action=wbsearchentities&format=json&type=item&uselang=en")
    fun searchForDepicts(
        @Query("search") query: String?,
        @Query("limit") limit: String?,
        @Query("language") language: String?,
        @Query("uselang") uselang: String?,
        @Query("continue") offset: String?
    ): Single<DepictSearchResponse>

    @GET("/w/api.php?format=json&action=wbgetentities")
    fun getEntities(@Query("ids") ids: String?): Single<Entities>
}
