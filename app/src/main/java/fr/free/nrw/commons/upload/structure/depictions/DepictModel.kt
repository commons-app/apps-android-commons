package fr.free.nrw.commons.upload.structure.depictions

import android.content.Context
import androidx.preference.PreferenceManager
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.settings.Prefs
import io.github.coordinates2country.Coordinates2Country
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
    fun searchAllEntities(query: String, repository: UploadRepository): Flowable<List<DepictedItem>> {
        return if (query.isBlank()) {
            nearbyPlaces.switchMap { places: List<Place> ->
                val qids = mutableSetOf<String>()
                for(place in  places) {
                    place.wikiDataEntityId?.let { qids.add(it) }
                }
                repository.uploads.forEach { item ->
                    if(item.gpsCoords != null && item.gpsCoords.imageCoordsExists) {
                        Coordinates2Country.countryQID(item.gpsCoords.decLatitude,
                            item.gpsCoords.decLongitude)?.let { qids.add("Q$it") }
                    }
                }
                getPlaceDepictions(ArrayList(qids)).toFlowable()
            }
        } else {
            networkItems(query)
        }
    }

    /**
     * Provides [DepictedItem] instances via a [Single] for a given list of ids, providing an
     * empty list if no places/country are provided or if there is an error
     */
    fun getPlaceDepictions(qids: List<String>): Single<List<DepictedItem>> =
        qids.toIds().let { ids ->
            if (ids.isNotEmpty())
                depictsClient.getEntities(ids)
                    .map{
                        it.entities()
                            .values
                            .mapIndexed { index, entity ->  DepictedItem(entity)}
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


    fun getSavedLanguage(context: Context): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(Prefs.APP_UI_LANGUAGE, "en")
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

private fun List<String>.toIds() = mapNotNull { it }.joinToString("|")

private fun <T> Single<List<T>>.onErrorResumeWithEmptyList() = onErrorResumeNext { t: Throwable ->
    Single.just(emptyList<T>()).also { Timber.e(t) }
}
