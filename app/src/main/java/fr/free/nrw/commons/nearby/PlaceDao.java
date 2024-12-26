package fr.free.nrw.commons.nearby;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Completable;
import java.util.List;

/**
 * Data Access Object (DAO) for accessing the Place entity in the database.
 * This class provides methods for storing and retrieving Place objects,
 * utilized for the caching of places in the Nearby Map feature.
 */
@Dao
public abstract class PlaceDao {

    /**
     * Inserts a Place object into the database.
     * If a conflict occurs, the existing entry will be replaced.
     *
     * @param place The Place object to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void saveSynchronous(Place place);

    /**
     * Retrieves a Place object from the database based on the provided entity ID.
     *
     * @param entity The entity ID of the Place to be retrieved.
     * @return The Place object with the specified entity ID.
     */
    @Query("SELECT * from place WHERE entityID=:entity")
    public abstract Place getPlace(String entity);

    @Query("SELECT * from place WHERE latitude>=:latBegin AND longitude>=:lngBegin "
        + "AND latitude<:latEnd AND longitude<:lngEnd")
    public abstract List<Place> fetchPlaces(double latBegin, double lngBegin,
        double latEnd, double lngEnd);

    /**
     * Saves a Place object asynchronously into the database.
     */
    public Completable save(final Place place) {
        return Completable
            .fromAction(() -> saveSynchronous(place));
    }

    /**
     * Deletes all Place objects from the database.
     */
    @Query("DELETE FROM place")
    public abstract void deleteAllSynchronous();

    /**
     * Deletes all Place objects from the database.
     *
     * @return A Completable that completes once the deletion operation is done.
     */
    public Completable deleteAll() {
        return Completable.fromAction(this::deleteAllSynchronous);
    }
}
