package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The model class for depictions in upload
 */
@Singleton
class DepictModel @Inject constructor(private val depictsInterface: DepictsInterface) {

    val nearbyPlaces: BehaviorProcessor<List<Place>> = BehaviorProcessor.createDefault(emptyList())

    companion object {
        private const val SEARCH_DEPICTS_LIMIT = 25
    }

    /**
     * Search for depictions
     */
    fun searchAllEntities(query: String): Flowable<List<DepictedItem>> {
        if (query.isBlank()) {
            return nearbyPlaces.map { it.map(::DepictedItem) }
        }
        return networkItems(query)
    }

    private fun networkItems(query: String): Flowable<List<DepictedItem>> {
        val language = Locale.getDefault().language
        return depictsInterface
            .searchForDepicts(query, "$SEARCH_DEPICTS_LIMIT", language, language, "0")
            .map { it.search.map(::DepictedItem) }
            .toFlowable()
    }

    fun cleanUp() {
        nearbyPlaces.offer(emptyList())
    }

}
