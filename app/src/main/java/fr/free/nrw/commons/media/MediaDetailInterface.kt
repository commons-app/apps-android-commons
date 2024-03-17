package fr.free.nrw.commons.media

import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface for interacting with Commons Structured Data related APIs
 */
interface MediaDetailInterface {
    /**
     * Fetches entity using file name
     *
     * @param filename name of the file to be used for fetching captions
     */
    @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
    fun fetchEntitiesByFileName(
        @Query("languages") language: String?,
        @Query("titles") filename: String?
    ): Observable<Entities>

    /**
     * Gets labels for Depictions using Entity Id from MediaWikiAPI
     * @param entityId  EntityId (Ex: Q81566) of the depict entity
     */
    @GET("/w/api.php?format=json&action=wbgetentities&props=labels&languagefallback=1")
    fun getEntity(@Query("ids") entityId: String?): Single<Entities>

    /**
     * Fetches caption using wikibaseIdentifier
     *
     * @param wikibaseIdentifier pageId for the media
     */
    @GET("/w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
    fun getEntityForImage(
        @Query("languages") language: String?,
        @Query("ids") wikibaseIdentifier: String?
    ): Observable<Entities>

    /**
     * Fetches current wikitext
     * @param title file name
     * @return Single<MwQueryResponse>
    </MwQueryResponse> */
    @GET(WikidataConstants.MW_API_PREFIX + "action=query&prop=revisions&rvprop=content|timestamp&rvlimit=1&converttitles=")
    fun getWikiText(
        @Query("titles") title: String?
    ): Single<MwQueryResponse>
}
