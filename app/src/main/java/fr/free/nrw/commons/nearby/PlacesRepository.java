package fr.free.nrw.commons.nearby;

import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;
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

}
