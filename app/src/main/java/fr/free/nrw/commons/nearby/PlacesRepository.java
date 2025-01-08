package fr.free.nrw.commons.nearby;

import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;

/**
 * The PlacesRepository class acts as a repository for Place entities.
 * It interacts with the PlacesLocalDataSource to perform database operations.
 */
public class PlacesRepository {

    private PlacesLocalDataSource localDataSource;

    @Inject
    public PlacesRepository(PlacesLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    /**
     * Saves a Place object asynchronously into the database.
     *
     * @param place The Place object to be saved.
     * @return A Completable that completes once the save operation is done.
     */
    public Completable save(Place place){
        return localDataSource.savePlace(place);
    }

    /**
     * Fetches a Place object from the database based on the provided entity ID.
     *
     * @param entityID The entity ID of the Place to be retrieved.
     * @return The Place object with the specified entity ID.
     */
    public Place fetchPlace(String entityID){
        return localDataSource.fetchPlace(entityID);
    }

    /**
     * Retrieves a list of places within the geographical area specified by map's opposite corners.
     *
     * @param mapBottomLeft Bottom left corner of the map.
     * @param mapTopRight Top right corner of the map.
     * @return The list of saved places within the map's view.
     */
    public List<Place> fetchPlaces(final LatLng mapBottomLeft, final LatLng mapTopRight) {
        return localDataSource.fetchPlaces(mapBottomLeft, mapTopRight);
    }

    /**
     * Clears the Nearby cache on an IO thread.
     *
     * @return A Completable that completes once the cache has been successfully cleared.
     */
    public Completable clearCache() {
        return localDataSource.clearCache()
            .subscribeOn(Schedulers.io()); // Ensure it runs on IO thread
    }
}
