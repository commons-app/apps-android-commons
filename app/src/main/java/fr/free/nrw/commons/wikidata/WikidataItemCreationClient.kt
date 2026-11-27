package fr.free.nrw.commons.wikidata

import com.google.gson.Gson
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.di.NetworkingModule
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Creates a new Wikidata item for a Wikipedia article that has no item yet.
 */
@Singleton
class WikidataItemCreationClient
    @Inject
    constructor(
        private val wikidataInterface: WikidataInterface,
        private val gson: Gson,
        @param:Named(NetworkingModule.NAMED_WIKI_CSRF) private val csrfTokenClient: CsrfTokenClient,
    ) {
        /**
         * Creates an item with a label and sitelink for the given Wikipedia article and returns its QID.
         */
        fun createItem(
            languageCode: String,
            title: String,
        ): Single<String> =
            Single
                .fromCallable { csrfTokenClient.getTokenBlocking() }
                .flatMap { token -> wikidataInterface.postCreateItem(itemData(languageCode, title), token) }
                .map { response ->
                    response.entity
                        ?.id()
                        ?.takeIf { it.startsWith("Q") }
                        ?: error("Wikidata did not return an item ID")
                }

        private fun itemData(
            languageCode: String,
            title: String,
        ): String {
            val site = "${languageCode}wiki"
            return gson.toJson(
                mapOf(
                    "labels" to mapOf(languageCode to mapOf("language" to languageCode, "value" to title)),
                    "sitelinks" to mapOf(site to mapOf("site" to site, "title" to title)),
                ),
            )
        }
    }
