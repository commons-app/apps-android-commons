package fr.free.nrw.commons.explore.map;

import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.nearby.Place;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import timber.log.Timber;

public class ExploreMapController extends MapController {
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

    @Override
    public NearbyPlacesInfo loadAttractionsFromLocation(LatLng curLatLng,
        LatLng searchLatLng, boolean returnClosestResult,
        boolean checkingAroundCurrentLocation, boolean shouldQueryForMonuments) {

        if (searchLatLng == null) {
            Timber.d("Loading attractions nearby, but curLatLng is null");
            return null;
        }
        List<Place> places = explorePlaces.radiusExpander(searchLatLng, Locale.getDefault().getLanguage(), returnClosestResult,
                shouldQueryForMonuments, customQuery);
        return null;
    }
}
