package fr.free.nrw.commons.explore.map;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import javax.inject.Inject;

public class ExploreMapController {
    private static final int MAX_RESULTS = 1000;
    private final ExplorePlaces explorePlaces;
    public static double currentLocationSearchRadius = 10.0; //in kilometers
    public static LatLng currentLocation; // Users latest fetched location
    public static LatLng latestSearchLocation; // Can be current and camera target on search this area button is used
    public static double latestSearchRadius = 10.0; // Any last search radius except closest result search


    @Inject
    public ExploreMapController(ExplorePlaces explorePlaces) {
        this.explorePlaces = explorePlaces;
    }
}
