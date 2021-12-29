package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.mwapi.Binding
import fr.free.nrw.commons.mwapi.SparqlResponse
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.model.DepictSearchItem
import io.reactivex.Single
import org.wikipedia.wikidata.Entities
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Depicts Client to handle custom calls to Commons Wikibase APIs
 */
@Singleton
class DepictsClient @Inject constructor(private val depictsInterface: DepictsInterface) {

    /**
     * Search for depictions using the search item
     * @return list of depicted items
     */
    fun searchForDepictions(query: String?, limit: Int, offset: Int): Single<List<DepictedItem>> {
        val language = Locale.getDefault().language
        return depictsInterface.searchForDepicts(query, "$limit", language, language, "$offset")
            .map { it.search.joinToString("|", transform = DepictSearchItem::id) }
            .mapToDepictions()
    }

    fun getEntities(ids: String): Single<Entities> {
        return depictsInterface.getEntities(ids)
    }

    fun toDepictions(sparqlResponse: Single<SparqlResponse>): Single<List<DepictedItem>> {
        return sparqlResponse.map {
            it.results.bindings.joinToString("|", transform = Binding::id)
        }.mapToDepictions()
    }

    private fun Single<String>.mapToDepictions() =
        flatMap(::getEntities)
        .map { it.entities().values.map(::DepictedItem) }
}
