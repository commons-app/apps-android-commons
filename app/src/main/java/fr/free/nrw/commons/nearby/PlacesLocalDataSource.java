package fr.free.nrw.commons.nearby;

import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

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
     * Retrieves a list of places from the database within the geographical area
     * specified by map's opposite corners.
     *
     * @param mapBottomLeft Bottom left corner of the map.
     * @param mapTopRight Top right corner of the map.
     * @return The list of saved places within the map's view.
     */
    public List<Place> fetchPlaces(final LatLng mapBottomLeft, final LatLng mapTopRight) {
        class Constraint {

            final double latBegin;
            final double lngBegin;
            final double latEnd;
            final double lngEnd;

            public Constraint(final double latBegin, final double lngBegin, final double latEnd,
                final double lngEnd) {
                this.latBegin = latBegin;
                this.lngBegin = lngBegin;
                this.latEnd = latEnd;
                this.lngEnd = lngEnd;
            }
        }

        final List<Constraint> constraints = new ArrayList<>();

        if (mapTopRight.getLatitude() < mapBottomLeft.getLatitude()) {
            if (mapTopRight.getLongitude() < mapBottomLeft.getLongitude()) {
                constraints.add(
                    new Constraint(mapBottomLeft.getLatitude(), mapBottomLeft.getLongitude(), 90.0,
                        180.0));
                constraints.add(new Constraint(mapBottomLeft.getLatitude(), -180.0, 90.0,
                    mapTopRight.getLongitude()));
                constraints.add(
                    new Constraint(-90.0, mapBottomLeft.getLongitude(), mapTopRight.getLatitude(),
                        180.0));
                constraints.add(new Constraint(-90.0, -180.0, mapTopRight.getLatitude(),
                    mapTopRight.getLongitude()));
            } else {
                constraints.add(
                    new Constraint(mapBottomLeft.getLatitude(), mapBottomLeft.getLongitude(), 90.0,
                        mapTopRight.getLongitude()));
                constraints.add(
                    new Constraint(-90.0, mapBottomLeft.getLongitude(), mapTopRight.getLatitude(),
                        mapTopRight.getLongitude()));
            }
        } else {
            if (mapTopRight.getLongitude() < mapBottomLeft.getLongitude()) {
                constraints.add(
                    new Constraint(mapBottomLeft.getLatitude(), mapBottomLeft.getLongitude(),
                        mapTopRight.getLatitude(), 180.0));
                constraints.add(
                    new Constraint(mapBottomLeft.getLatitude(), -180.0, mapTopRight.getLatitude(),
                        mapTopRight.getLongitude()));
            } else {
                constraints.add(
                    new Constraint(mapBottomLeft.getLatitude(), mapBottomLeft.getLongitude(),
                        mapTopRight.getLatitude(), mapTopRight.getLongitude()));
            }
        }

        final List<Place> cachedPlaces = new ArrayList<>();
        for (final Constraint constraint : constraints) {
            cachedPlaces.addAll(placeDao.fetchPlaces(
                constraint.latBegin,
                constraint.lngBegin,
                constraint.latEnd,
                constraint.lngEnd
            ));
        }

        return cachedPlaces;
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

    public Completable clearCache() {
        return placeDao.deleteAll();
    }
}
