package fr.free.nrw.commons.nearby

import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Loads the localized names of the Nearby place types from Wikidata.
 *
 * Each place type is a Wikidata item, so its label already exists in most languages.
 * The labels are fetched once per language and stored, as they practically never change.
 */
@Singleton
class PlaceTypeLabelsClient
    @Inject
    constructor(
        private val depictsInterface: DepictsInterface,
        @param:Named("default_preferences") private val kvStore: JsonKvStore,
    ) {
        /**
         * @param language language code, e.g. "fr"
         * @return map of Q-ID -> localized name, from the cache when available
         */
        fun getLabels(language: String): Single<Map<String, String>> {
            val qids = Label.getQids()
            val cached = kvStore.getJson<Map<String, String>>(cacheKey(language))
            if (cached != null && cached.keys.containsAll(qids)) {
                return Single.just(cached)
            }
            return Single
                .concat(qids.chunked(MAX_IDS_PER_REQUEST).map { fetchLabels(it, language) })
                .reduce(emptyMap<String, String>()) { all, chunk -> all + chunk }
                .doOnSuccess { kvStore.putJson(cacheKey(language), it) }
        }

        private fun fetchLabels(
            qids: List<String>,
            language: String,
        ): Single<Map<String, String>> =
            depictsInterface
                .getLabels(qids.joinToString("|"), language)
                .map { response ->
                    // Keep empty names too, so a type without any label is not refetched forever
                    qids.associateWith { qid ->
                        response.entities()[qid]?.labels()?.get(language)?.value().orEmpty()
                    }
                }

        private fun cacheKey(language: String) = "$CACHE_KEY_PREFIX$language"

        companion object {
            private const val CACHE_KEY_PREFIX = "nearby_place_type_labels_"
            private const val MAX_IDS_PER_REQUEST = 50
        }
    }
