package fr.free.nrw.commons.nearby

import fr.free.nrw.commons.location.LatLng
import io.reactivex.Completable
import javax.inject.Inject

/**
 * The LocalDataSource class for Places
 */
class PlacesLocalDataSource @Inject constructor(private val placeDao: PlaceDao) {
    /**
     * Fetches a Place object from the database based on the provided entity ID.
     *
     * @param entityID The entity ID of the Place to be retrieved.
     * @return The Place object with the specified entity ID.
     */
    fun fetchPlace(entityID: String?): Place? = placeDao.getPlace(entityID)

    /**
     * Retrieves a list of places from the database within the geographical area
     * specified by map's opposite corners.
     *
     * @param mapBottomLeft Bottom left corner of the map.
     * @param mapTopRight Top right corner of the map.
     * @return The list of saved places within the map's view.
     */
    fun fetchPlaces(mapBottomLeft: LatLng, mapTopRight: LatLng): List<Place> {
        class Constraint(val latBegin: Double, val lngBegin: Double, val latEnd: Double, val lngEnd: Double)

        val constraints: MutableList<Constraint> = mutableListOf()

        if (mapTopRight.latitude < mapBottomLeft.latitude) {
            if (mapTopRight.longitude < mapBottomLeft.longitude) {
                constraints.add(Constraint(mapBottomLeft.latitude, mapBottomLeft.longitude, 90.0, 180.0))
                constraints.add(Constraint(mapBottomLeft.latitude, -180.0, 90.0, mapTopRight.longitude))
                constraints.add(Constraint(-90.0, mapBottomLeft.longitude, mapTopRight.latitude, 180.0))
                constraints.add(Constraint(-90.0, -180.0, mapTopRight.latitude, mapTopRight.longitude))
            } else {
                constraints.add(Constraint(mapBottomLeft.latitude, mapBottomLeft.longitude, 90.0, mapTopRight.longitude))
                constraints.add(Constraint(-90.0, mapBottomLeft.longitude, mapTopRight.latitude, mapTopRight.longitude))
            }
        } else {
            if (mapTopRight.longitude < mapBottomLeft.longitude) {
                constraints.add(Constraint(mapBottomLeft.latitude, mapBottomLeft.longitude, mapTopRight.latitude, 180.0))
                constraints.add(Constraint(mapBottomLeft.latitude, -180.0, mapTopRight.latitude, mapTopRight.longitude))
            } else {
                constraints.add(Constraint(mapBottomLeft.latitude, mapBottomLeft.longitude, mapTopRight.latitude, mapTopRight.longitude))
            }
        }

        val cachedPlaces: List<Place> = buildList {
            for (constraint in constraints) {
                addAll(placeDao.fetchPlaces(constraint.latBegin, constraint.lngBegin, constraint.latEnd, constraint.lngEnd)!!)
            }
        }

        return cachedPlaces
    }

    /**
     * Saves a Place object asynchronously into the database.
     *
     * @param place The Place object to be saved.
     * @return A Completable that completes once the save operation is done.
     */
    fun savePlace(place: Place): Completable = placeDao.save(place)

    fun clearCache(): Completable = placeDao.deleteAll()
}
