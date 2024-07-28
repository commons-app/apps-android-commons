package fr.free.nrw.commons.nearby;

import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;
import javax.inject.Inject;

/**
 * The LocalDataSource class for Places
 */
public class PlacesLocalDataSource {

    private final PlaceDao placeDao;

    @Inject
    public PlacesLocalDataSource(
        final PlaceDao placeDao) {
        this.placeDao = placeDao;
    }

    /**
     * Fetches a Place object from the database based on the provided entity ID.
     *
     * @param entityID The entity ID of the Place to be retrieved.
     * @return The Place object with the specified entity ID.
     */
    public Place fetchPlace(String entityID){
        return placeDao.getPlace(entityID);
    }

    /**
     * Saves a Place object asynchronously into the database.
     *
     * @param place The Place object to be saved.
     * @return A Completable that completes once the save operation is done.
     */
    public Completable savePlace(Place place) {
        return placeDao.save(place);
    }
}
