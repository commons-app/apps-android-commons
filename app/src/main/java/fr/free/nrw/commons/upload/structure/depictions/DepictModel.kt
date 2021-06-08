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
                depictsClient.getEntities(places.toIds())
                    .map {
                        it.entities()
                            .values
                            .mapIndexed { index, entity -> DepictedItem(entity, places[index]) }
                    }
                    .onErrorResumeWithEmptyList()
                    .toFlowable()
            }
        else
            networkItems(query)
    }

    /**
     * Provides a [DepictedItem] for a given [Place] via an [Observable], returning an empty
     * observable if the given place is null or has no entity id
     */
    fun getPlaceDepiction(place: Place?): Observable<DepictedItem> {
        return place?.wikiDataEntityId?.let { id ->
                depictsClient.getEntities(id)
                    .flatMapObservable { Observable.fromIterable(it.entities().values) }
                    .map { DepictedItem(it, place) }
            } ?: Observable.empty()
    }

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
