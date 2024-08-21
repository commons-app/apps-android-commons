package fr.free.nrw.commons.nearby;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;

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

    /**
     * Saves a Place object asynchronously into the database.
     */
    public Completable save(final Place place) {
        return Completable
            .fromAction(() -> {
                saveSynchronous(place);
            });
    }
}
