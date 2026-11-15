package fr.free.nrw.commons.wikidata

import com.google.gson.Gson
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.StatementPartial
import fr.free.nrw.commons.wikidata.model.WbCreateClaimResponse
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WikidataClient
    @Inject
    constructor(
        private val wikidataInterface: WikidataInterface,
        private val gson: Gson,
    ) {
        /** Returns the entity from the current Wikidata response for this item. */
        internal fun getItemForImageEdit(id: String): Entities.Entity =
            checkNotNull(wikidataInterface.getItemForImageEdit(id).blockingGet().getEntityMap()[id])

        /**
         * Create wikidata claim to add P18 value
         *
         * @return revisionID of the edit
         */
        fun setClaim(
            claim: StatementPartial?,
            tags: String?,
            baseRevision: Long,
        ): Observable<Long> =
            csrfToken()
                .flatMap { csrfToken: String? ->
                    wikidataInterface.postSetClaim(gson.toJson(claim), tags!!, csrfToken!!, baseRevision)
                }.map { mwPostResponse: WbCreateClaimResponse -> mwPostResponse.pageinfo.lastrevid }

        /**
         * Get csrf token for wikidata edit
         */
        private fun csrfToken(): Observable<String?> = wikidataInterface.getCsrfToken().map { it.query()?.csrfToken() }
    }
