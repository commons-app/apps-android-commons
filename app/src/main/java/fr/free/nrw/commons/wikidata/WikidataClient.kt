package fr.free.nrw.commons.wikidata

import com.google.gson.Gson
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
        /**
         * Create wikidata claim to add P18 value
         *
         * @return revisionID of the edit
         */
        fun setClaim(
            claim: StatementPartial?,
            tags: String?,
        ): Observable<Long> =
            csrfToken()
                .flatMap { csrfToken: String? ->
                    wikidataInterface.postSetClaim(gson.toJson(claim), tags!!, csrfToken!!)
                }.map { mwPostResponse: WbCreateClaimResponse -> mwPostResponse.pageinfo.lastrevid }

        /**
         * Get csrf token for wikidata edit
         */
        private fun csrfToken(): Observable<String?> = wikidataInterface.getCsrfToken().map { it.query()?.csrfToken() }
    }
