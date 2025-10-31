package fr.free.nrw.commons.nearby

import fr.free.nrw.commons.location.LatLng
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * The PlacesRepository class acts as a repository for Place entities.
 * It interacts with the PlacesLocalDataSource to perform database operations.
 */
class PlacesRepository @Inject constructor(private val localDataSource: PlacesLocalDataSource) {
    /**
     * Saves a Place object asynchronously into the database.
     *
     * @param place The Place object to be saved.
     * @return A Completable that completes once the save operation is done.
     */
    fun save(place: Place?): Completable = localDataSource.savePlace(place)

    /**
     * Fetches a Place object from the database based on the provided entity ID.
     *
     * @param entityID The entity ID of the Place to be retrieved.
     * @return The Place object with the specified entity ID.
     */
    fun fetchPlace(entityID: String): Place = localDataSource.fetchPlace(entityID)

    /**
     * Retrieves a list of places within the geographical area specified by map's opposite corners.
     *
     * @param mapBottomLeft Bottom left corner of the map.
     * @param mapTopRight Top right corner of the map.
     * @return The list of saved places within the map's view.
     */
    fun fetchPlaces(mapBottomLeft: LatLng, mapTopRight: LatLng): List<Place> =
        localDataSource.fetchPlaces(mapBottomLeft, mapTopRight) ?: emptyList()

    /**
     * Clears the Nearby cache on an IO thread.
     *
     * @return A Completable that completes once the cache has been successfully cleared.
     */
    fun clearCache(): Completable = localDataSource.clearCache()
        .subscribeOn(Schedulers.io())
}
