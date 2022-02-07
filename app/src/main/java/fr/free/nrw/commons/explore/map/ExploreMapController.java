package fr.free.nrw.commons.explore.map;

import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.media.MediaClient;
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
    MediaClient mediaClient;

    public ExploreMapController(ExplorePlaces explorePlaces) {
        this.explorePlaces = explorePlaces;
    }

    public NearbyPlacesInfo loadAttractionsFromLocation(LatLng curLatLng, LatLng searchLatLng, boolean checkingAroundCurrentLocation) {

        // TODO: check nearbyPlacesInfo in NearbyController for search this area logic

        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but curLatLng is null");
            return null;
        }

        NearbyPlacesInfo nearbyPlacesInfo = new NearbyPlacesInfo();
        try {
            List<Media> mediaList = explorePlaces.limitExpander(searchLatLng, 500);
            nearbyPlacesInfo.placeList = mediaToExplorePlace(mediaList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nearbyPlacesInfo;
    }

    private List<Place> mediaToExplorePlace( List<Media> mediaList) {
        return null;
    }

}
