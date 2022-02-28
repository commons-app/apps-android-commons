package fr.free.nrw.commons.explore.depictions

import android.annotation.SuppressLint
import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import fr.free.nrw.commons.data.models.upload.depictions.get
import fr.free.nrw.commons.mwapi.Binding
import fr.free.nrw.commons.mwapi.SparqlResponse
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import fr.free.nrw.commons.wikidata.WikidataProperties
import fr.free.nrw.commons.wikidata.model.DepictSearchItem
import io.reactivex.Single
import org.wikipedia.wikidata.DataValue
import org.wikipedia.wikidata.Entities
import org.wikipedia.wikidata.Statement_partial
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

    /**
     * Fetches Entities from ids ex. "Q1233|Q546" and converts them into DepictedItem
     */
    @SuppressLint("CheckResult")
    private fun Single<String>.mapToDepictions() =
        flatMap(::getEntities)
        .map { entities ->
            entities.entities().values.map { entity ->
                if (entity.descriptions().byLanguageOrFirstOrEmpty() == "") {
                    val entities: Entities = getEntities(entity[WikidataProperties.INSTANCE_OF]
                        .toIds()[0]).blockingGet()
                    val nameAsDescription = entities.entities().values.first().labels()
                        .byLanguageOrFirstOrEmpty()
                    DepictedItem(
                        entity,
                        entity.labels().byLanguageOrFirstOrEmpty(),
                        nameAsDescription
                    )
                } else {
                    DepictedItem(
                        entity,
                        entity.labels().byLanguageOrFirstOrEmpty(),
                        entity.descriptions().byLanguageOrFirstOrEmpty()
                    )
                }
            }
        }

    /**
     * Tries to get Entities.Label by default language from the map.
     * If that returns null, Tries to retrieve first element from the map.
     * If that still returns null, function returns "".
     */
    private fun Map<String, Entities.Label>.byLanguageOrFirstOrEmpty() =
        let {
            it[Locale.getDefault().language] ?: it.values.firstOrNull() }?.value() ?: ""

    /**
     * returns list of id ex. "Q2323" from Statement_partial
     */
    private fun List<Statement_partial>?.toIds(): List<String> {
        return this?.map { it.mainSnak.dataValue }
            ?.filterIsInstance<DataValue.EntityId>()
            ?.map { it.value.id }
            ?: emptyList()
    }
}
