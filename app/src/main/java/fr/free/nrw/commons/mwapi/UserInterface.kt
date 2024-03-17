package fr.free.nrw.commons.mwapi

import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface UserInterface {
    /**
     * Gets the log events of user
     * @param user name of user without prefix
     * @param continuation continuation params returned in previous query
     * @return query response
     */
    @GET(WikidataConstants.MW_API_PREFIX + "action=query&list=logevents&letype=upload&leprop=title|timestamp|ids&lelimit=500")
    fun getUserLogEvents(
        @Query("leuser") user: String?,
        @QueryMap continuation: Map<String?, String?>?
    ): Observable<MwQueryResponse>

    /**
     * Checks to see if a user is currently blocked from Commons
     */
    @GET(WikidataConstants.MW_API_PREFIX + "action=query&meta=userinfo&uiprop=blockinfo")
    fun getUserBlockInfo(): Observable<MwQueryResponse>
}
