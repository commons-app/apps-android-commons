package fr.free.nrw.commons.nearby;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.location.LatLng;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import timber.log.Timber;

public class NearbyController extends MapController {

    private static final int MAX_RESULTS = 1000;
    private final NearbyPlaces nearbyPlaces;
    public static double currentLocationSearchRadius = 10.0; //in kilometers
    public static LatLng currentLocation; // Users latest fetched location
    public static LatLng latestSearchLocation; // Can be current and camera target on search this area button is used
    public static double latestSearchRadius = 10.0; // Any last search radius except closest result search

    public static List<MarkerPlaceGroup> markerLabelList = new ArrayList<>();

    @Inject
    public NearbyController(NearbyPlaces nearbyPlaces) {
        this.nearbyPlaces = nearbyPlaces;
    }


    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param currentLatLng       current location for user
     * @param searchLatLng        the location user wants to search around
     * @param returnClosestResult if this search is done to find closest result or all results
     * @param customQuery         if this search is done via an advanced query
     * @return NearbyPlacesInfo a variable holds Place list without distance information and
     * boundary coordinates of current Place List
     */
    public NearbyPlacesInfo loadAttractionsFromLocation(final LatLng currentLatLng,
        final LatLng searchLatLng,
        final boolean returnClosestResult, final boolean checkingAroundCurrentLocation,
        @Nullable final String customQuery) throws Exception {

        Timber.d("Loading attractions near %s", searchLatLng);
        NearbyPlacesInfo nearbyPlacesInfo = new NearbyPlacesInfo();
        if (searchLatLng == null) {
            Timber.d("Loading attractions nearby, but currentLatLng is null");
            return null;
        }
        List<Place> places = nearbyPlaces
            .radiusExpander(searchLatLng, Locale.getDefault().getLanguage(), returnClosestResult,
                customQuery);

        if (null != places && places.size() > 0) {
            LatLng[] boundaryCoordinates = {
                places.get(0).location, // south
                places.get(0).location, // north
                places.get(0).location, // west
                places.get(0).location};// east, init with a random location

            if (currentLatLng != null) {
                Timber.d("Sorting places by distance...");
                final Map<Place, Double> distances = new HashMap<>();
                for (Place place : places) {
                    distances.put(place, computeDistanceBetween(place.location, currentLatLng));
                    // Find boundaries with basic find max approach
                    if (place.location.getLatitude() < boundaryCoordinates[0].getLatitude()) {
                        boundaryCoordinates[0] = place.location;
                    }
                    if (place.location.getLatitude() > boundaryCoordinates[1].getLatitude()) {
                        boundaryCoordinates[1] = place.location;
                    }
                    if (place.location.getLongitude() < boundaryCoordinates[2].getLongitude()) {
                        boundaryCoordinates[2] = place.location;
                    }
                    if (place.location.getLongitude() > boundaryCoordinates[3].getLongitude()) {
                        boundaryCoordinates[3] = place.location;
                    }
                }
                Collections.sort(places,
                    (lhs, rhs) -> {
                        double lhsDistance = distances.get(lhs);
                        double rhsDistance = distances.get(rhs);
                        return (int) (lhsDistance - rhsDistance);
                    }
                );
            }
            nearbyPlacesInfo.currentLatLng = currentLatLng;
            nearbyPlacesInfo.searchLatLng = searchLatLng;
            nearbyPlacesInfo.placeList = places;
            nearbyPlacesInfo.boundaryCoordinates = boundaryCoordinates;

            // Returning closes result means we use the controller for nearby card. So no need to set search this area flags
            if (!returnClosestResult) {
                // To remember latest search either around user or any point on map
                latestSearchLocation = searchLatLng;
                latestSearchRadius = nearbyPlaces.radius * 1000; // to meter

                // Our radius searched around us, will be used to understand when user search their own location, we will follow them
                if (checkingAroundCurrentLocation) {
                    currentLocationSearchRadius = nearbyPlaces.radius * 1000; // to meter
                    currentLocation = currentLatLng;
                }
            }


        }
        return nearbyPlacesInfo;
    }

    public String getPlacesAsKML(LatLng currentLocation) throws Exception {
        return nearbyPlaces.getPlacesAsKML(
            calculateSouthWest(currentLocation.getLatitude(), currentLocation.getLongitude(), 10),
            calculateNorthEast(currentLocation.getLatitude(), currentLocation.getLongitude(), 10)
        );
    }

    public String getPlacesAsGPX(LatLng currentLocation) throws Exception {
        return nearbyPlaces.getPlacesAsGPX(
            calculateSouthWest(currentLocation.getLatitude(), currentLocation.getLongitude(), 10),
            calculateNorthEast(currentLocation.getLatitude(), currentLocation.getLongitude(), 10)
        );
    }

    /**
     * Retrieves a list of places based on the provided list of places and language.
     *
     * @param placeList A list of Place objects for which to fetch information.
     * @return A list of Place objects obtained from the Wikidata query.
     * @throws Exception If an error occurs during the retrieval process.
     */
    public List<Place> getPlaces(List<Place> placeList) throws Exception {
        return nearbyPlaces.getPlaces(placeList, Locale.getDefault().getLanguage());
    }

    public static LatLng calculateNorthEast(double latitude, double longitude, double distance) {
        double lat1 = Math.toRadians(latitude);
        double deltaLat = distance * 0.008;
        double deltaLon = distance / Math.cos(lat1)*0.008;
        double lat2 = latitude + deltaLat;
        double lon2 = longitude + deltaLon;

        return new LatLng(lat2, lon2, 0);
    }

    public static LatLng calculateSouthWest(double latitude, double longitude, double distance) {
        double lat1 = Math.toRadians(latitude);
        double deltaLat = distance * 0.008;
        double deltaLon = distance / Math.cos(lat1)*0.008;
        double lat2 = latitude - deltaLat;
        double lon2 = longitude - deltaLon;

        return new LatLng(lat2, lon2, 0);
    }

    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param currentLatLng                 The current latitude and longitude.
     * @param screenTopRight                The top right corner of the screen (latitude,
     *                                      longitude).
     * @param screenBottomLeft              The bottom left corner of the screen (latitude,
     *                                      longitude).
     * @param searchLatLng                  The latitude and longitude of the search location.
     * @param returnClosestResult           Flag indicating whether to return the closest result.
     * @param checkingAroundCurrentLocation Flag indicating whether to check around the current
     *                                      location.
     * @param shouldQueryForMonuments       Flag indicating whether to include monuments in the
     *                                      query.
     * @param customQuery                   Optional custom SPARQL query to use instead of default
     *                                      queries.
     * @return An object containing information about nearby places.
     * @throws Exception If an error occurs during the retrieval process.
     */
    public NearbyPlacesInfo loadAttractionsFromLocation(final LatLng currentLatLng,
        final fr.free.nrw.commons.location.LatLng screenTopRight,
        final fr.free.nrw.commons.location.LatLng screenBottomLeft, final LatLng searchLatLng,
        final boolean returnClosestResult, final boolean checkingAroundCurrentLocation,
        final boolean shouldQueryForMonuments, @Nullable final String customQuery)
        throws Exception {

        Timber.d("Loading attractions near %s", searchLatLng);
        NearbyPlacesInfo nearbyPlacesInfo = new NearbyPlacesInfo();

        if (searchLatLng == null) {
            Timber.d("Loading attractions nearby, but currentLatLng is null");
            return null;
        }

        List<Place> places = nearbyPlaces.getFromWikidataQuery(screenTopRight, screenBottomLeft,
            Locale.getDefault().getLanguage(), shouldQueryForMonuments, customQuery);

        if (null != places && places.size() > 0) {
            LatLng[] boundaryCoordinates = {
                places.get(0).location, // south
                places.get(0).location, // north
                places.get(0).location, // west
                places.get(0).location};// east, init with a random location

            if (currentLatLng != null) {
                Timber.d("Sorting places by distance...");
                final Map<Place, Double> distances = new HashMap<>();
                for (Place place : places) {
                    distances.put(place, computeDistanceBetween(place.location, currentLatLng));
                    // Find boundaries with basic find max approach
                    if (place.location.getLatitude() < boundaryCoordinates[0].getLatitude()) {
                        boundaryCoordinates[0] = place.location;
                    }
                    if (place.location.getLatitude() > boundaryCoordinates[1].getLatitude()) {
                        boundaryCoordinates[1] = place.location;
                    }
                    if (place.location.getLongitude() < boundaryCoordinates[2].getLongitude()) {
                        boundaryCoordinates[2] = place.location;
                    }
                    if (place.location.getLongitude() > boundaryCoordinates[3].getLongitude()) {
                        boundaryCoordinates[3] = place.location;
                    }
                }
                Collections.sort(places,
                    (lhs, rhs) -> {
                        double lhsDistance = distances.get(lhs);
                        double rhsDistance = distances.get(rhs);
                        return (int) (lhsDistance - rhsDistance);
                    }
                );
            }
            nearbyPlacesInfo.currentLatLng = currentLatLng;
            nearbyPlacesInfo.searchLatLng = searchLatLng;
            nearbyPlacesInfo.placeList = places;
            nearbyPlacesInfo.boundaryCoordinates = boundaryCoordinates;

            // Returning closes result means we use the controller for nearby card. So no need to set search this area flags
            if (!returnClosestResult) {
                // To remember latest search either around user or any point on map
                latestSearchLocation = searchLatLng;
                latestSearchRadius = nearbyPlaces.radius * 1000; // to meter

                // Our radius searched around us, will be used to understand when user search their own location, we will follow them
                if (checkingAroundCurrentLocation) {
                    currentLocationSearchRadius = nearbyPlaces.radius * 1000; // to meter
                    currentLocation = currentLatLng;
                }
            }
        }
        return nearbyPlacesInfo;
    }

    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param currentLatLng       current location for user
     * @param searchLatLng        the location user wants to search around
     * @param returnClosestResult if this search is done to find closest result or all results
     * @return NearbyPlacesInfo a variable holds Place list without distance information and
     * boundary coordinates of current Place List
     */
    public NearbyPlacesInfo loadAttractionsFromLocation(final LatLng currentLatLng,
        final LatLng searchLatLng,
        final boolean returnClosestResult, final boolean checkingAroundCurrentLocation)
        throws Exception {
        return loadAttractionsFromLocation(currentLatLng, searchLatLng, returnClosestResult,
            checkingAroundCurrentLocation, null);
    }

    /**
     * Loads attractions from location for map view, we need to return BaseMarkerOption data type.
     *
     * @param currentLatLng users current location
     * @param placeList     list of nearby places in Place data type
     * @return BaseMarkerOptions list that holds nearby places
     */
    public static List<BaseMarker> loadAttractionsFromLocationToBaseMarkerOptions(
        LatLng currentLatLng,
        List<Place> placeList) {
        List<BaseMarker> baseMarkersList = new ArrayList<>();

        if (placeList == null) {
            return baseMarkersList;
        }
        placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));
        for (Place place : placeList) {
            BaseMarker baseMarker = new BaseMarker();
            String distance = formatDistanceBetween(currentLatLng, place.location);
            place.setDistance(distance);
            baseMarker.setTitle(place.name);
            baseMarker.setPosition(
                new fr.free.nrw.commons.location.LatLng(
                    place.location.getLatitude(),
                    place.location.getLongitude(), 0));
            baseMarker.setPlace(place);
            baseMarkersList.add(baseMarker);
        }
        return baseMarkersList;
    }


    /**
     * Updates makerLabelList item isBookmarked value
     *
     * @param place        place which is bookmarked
     * @param isBookmarked true is bookmarked, false if bookmark removed
     */
    @MainThread
    public static void updateMarkerLabelListBookmark(Place place, boolean isBookmarked) {
        for (ListIterator<MarkerPlaceGroup> iter = markerLabelList.listIterator();
            iter.hasNext(); ) {
            MarkerPlaceGroup markerPlaceGroup = iter.next();
            if (markerPlaceGroup.getPlace().getWikiDataEntityId()
                .equals(place.getWikiDataEntityId())) {
                iter.set(new MarkerPlaceGroup(isBookmarked, place));
            }
        }
    }
}
