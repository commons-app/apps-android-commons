package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.nearby.Place
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The model class for depictions in upload
 */
@Singleton
class DepictModel @Inject constructor(private val depictsClient: DepictsClient) {

    val nearbyPlaces: BehaviorProcessor<List<Place>> = BehaviorProcessor.createDefault(emptyList())


    companion object {
        private const val SEARCH_DEPICTS_LIMIT = 25
    }

    /**
     * Search for depictions
     */
    fun searchAllEntities(query: String): Flowable<List<DepictedItem>> {
        if (query.isBlank()) {
            return nearbyPlaces.switchMap { places: List<Place> ->
                depictsClient.getEntities(
                    places.mapNotNull { it.wikiDataEntityId }.joinToString("|")
                )
                    .map {
                        it.entities()!!.values.mapIndexed { index, entity ->
                            DepictedItem(entity, places[index])
                        }
                    }.toFlowable()
            }
        }
        return networkItems(query)
    }

    private fun networkItems(query: String): Flowable<List<DepictedItem>> {
        return depictsClient.searchForDepictions(query, SEARCH_DEPICTS_LIMIT, 0)
            .toFlowable()
    }

    fun cleanUp() {
        nearbyPlaces.offer(emptyList())
    }

}
