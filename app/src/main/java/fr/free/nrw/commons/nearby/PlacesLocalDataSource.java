package fr.free.nrw.commons.nearby;

import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;
import javax.inject.Inject;

public class PlacesLocalDataSource {

    private final PlaceDao placeDao;

    @Inject
    public PlacesLocalDataSource(
        final PlaceDao placeDao) {
        this.placeDao = placeDao;
    }

    public Place fetchPlace(String entityID){
        return placeDao.getPlace(entityID);
    }

    public Completable savePlace(Place place) {
        return placeDao.save(place);
    }
}
