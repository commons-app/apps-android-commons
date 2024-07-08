package fr.free.nrw.commons.nearby;

import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;
import javax.inject.Inject;

public class PlacesRepository {

    private PlacesLocalDataSource localDataSource;

    @Inject
    public PlacesRepository(PlacesLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    public Completable save(Place place){
        return localDataSource.savePlace(place);
    }

    public Place fetchPlace(String entityID){
        return localDataSource.fetchPlace(entityID);
    }

}
