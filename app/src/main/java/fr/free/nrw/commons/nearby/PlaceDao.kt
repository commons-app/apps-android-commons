package fr.free.nrw.commons.nearby

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.functions.Action

/**
 * Data Access Object (DAO) for accessing the Place entity in the database.
 * This class provides methods for storing and retrieving Place objects,
 * utilized for the caching of places in the Nearby Map feature.
 */
@Dao
abstract class PlaceDao {
    /**
     * Inserts a Place object into the database.
     * If a conflict occurs, the existing entry will be replaced.
     *
     * @param place The Place object to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveSynchronous(place: Place)

    /**
     * Retrieves a Place object from the database based on the provided entity ID.
     *
     * @param entity The entity ID of the Place to be retrieved.
     * @return The Place object with the specified entity ID.
     */
    @Query("SELECT * from place WHERE entityID=:entity")
    abstract fun getPlace(entity: String?): Place?

    /**
     * Retrieves a list of places within the specified rectangular area.
     *
     * @param latBegin Latitudinal lower bound
     * @param lngBegin Longitudinal lower bound
     * @param latEnd Latitudinal upper bound, should be greater than `latBegin`
     * @param lngEnd Longitudinal upper bound, should be greater than `lngBegin`
     * @return The list of places within the specified rectangular geographical area.
     */
    @Query("SELECT * from place WHERE name!='' AND latitude>=:latBegin AND longitude>=:lngBegin AND latitude<:latEnd AND longitude<:lngEnd")
    abstract fun fetchPlaces(latBegin: Double, lngBegin: Double, latEnd: Double, lngEnd: Double): List<Place>?

    /**
     * Saves a Place object asynchronously into the database.
     */
    fun save(place: Place): Completable = Completable.fromAction(Action { saveSynchronous(place) })

    /**
     * Deletes all Place objects from the database.
     */
    @Query("DELETE FROM place")
    abstract fun deleteAllSynchronous()

    /**
     * Deletes all Place objects from the database.
     *
     * @return A Completable that completes once the deletion operation is done.
     */
    fun deleteAll(): Completable = Completable.fromAction(Action { deleteAllSynchronous() })
}
