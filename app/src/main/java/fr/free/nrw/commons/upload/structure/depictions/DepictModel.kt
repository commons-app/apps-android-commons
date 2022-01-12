package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.nearby.Place
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import timber.log.Timber
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
        return if (query.isBlank())
            nearbyPlaces.switchMap { places: List<Place> ->
                getPlaceDepictions(places).toFlowable()
            }
        else
            networkItems(query)
    }

    /**
     * Provides [DepictedItem] instances via a [Single] for a given list of [Place], providing an
     * empty list if no places are provided or if there is an error
     */
    fun getPlaceDepictions(places: List<Place>): Single<List<DepictedItem>> =
        places.toIds().let { ids ->
            if (ids.isNotEmpty())
                depictsClient.getEntities(ids)
                    .map{
                        it.entities()
                            .values
                            .mapIndexed { index, entity ->  DepictedItem(entity, places[index])}
                    }
                    .onErrorResumeWithEmptyList()
            else Single.just(emptyList())
        }

    fun getDepictions(ids: String): Single<List<DepictedItem>> =
        if (ids.isNotEmpty())
            depictsClient.getEntities(ids)
                .map{
                    it.entities()
                        .values
                        .mapIndexed { _, entity ->  DepictedItem(entity)}
                }
                .onErrorResumeWithEmptyList()
        else Single.just(emptyList())


    private fun networkItems(query: String): Flowable<List<DepictedItem>> {
        return depictsClient.searchForDepictions(query, SEARCH_DEPICTS_LIMIT, 0)
            .onErrorResumeWithEmptyList()
            .toFlowable()
    }

    fun cleanUp() {
        nearbyPlaces.offer(emptyList())
    }

}

private fun List<Place>.toIds() = mapNotNull { it.wikiDataEntityId }.joinToString("|")

private fun <T> Single<List<T>>.onErrorResumeWithEmptyList() = onErrorResumeNext { t: Throwable ->
    Single.just(emptyList<T>()).also { Timber.e(t) }
}
