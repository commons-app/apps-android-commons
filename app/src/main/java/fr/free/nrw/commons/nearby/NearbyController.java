package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.graphics.drawable.VectorDrawableCompat;

import com.mapbox.mapboxsdk.annotations.IconFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.UiUtils;
import timber.log.Timber;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

public class NearbyController {
    private static final int MAX_RESULTS = 1000;
    private final NearbyPlaces nearbyPlaces;
    public static double searchedRadius = 10.0; //in kilometers
    public static LatLng currentLocation;

    @Inject
    public NearbyController(NearbyPlaces nearbyPlaces) {
        this.nearbyPlaces = nearbyPlaces;
    }


    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param curLatLng current location for user
     * @param latLangToSearchAround the location user wants to search around
     * @param returnClosestResult if this search is done to find closest result or all results
     * @return NearbyPlacesInfo a variable holds Place list without distance information
     * and boundary coordinates of current Place List
     */
    public NearbyPlacesInfo loadAttractionsFromLocation(LatLng curLatLng, LatLng latLangToSearchAround, boolean returnClosestResult, boolean checkingAroundCurrentLocation) throws IOException {

        Timber.d("Loading attractions near %s", latLangToSearchAround);
        NearbyPlacesInfo nearbyPlacesInfo = new NearbyPlacesInfo();

        if (latLangToSearchAround == null) {
            Timber.d("Loading attractions nearby, but curLatLng is null");
            return null;
        }
        List<Place> places = nearbyPlaces.radiusExpander(latLangToSearchAround, Locale.getDefault().getLanguage(), returnClosestResult);

        if (null != places && places.size() > 0) {
            LatLng[] boundaryCoordinates = {places.get(0).location,   // south
                    places.get(0).location, // north
                    places.get(0).location, // west
                    places.get(0).location};// east, init with a random location


            if (curLatLng != null) {
                Timber.d("Sorting places by distance...");
                final Map<Place, Double> distances = new HashMap<>();
                for (Place place : places) {
                    distances.put(place, computeDistanceBetween(place.location, curLatLng));
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
            nearbyPlacesInfo.placeList = places;
            nearbyPlacesInfo.boundaryCoordinates = boundaryCoordinates;
            if (!returnClosestResult && checkingAroundCurrentLocation) {
                // Do not update searched radius, if controller is used for nearby card notification
                searchedRadius = nearbyPlaces.radius;
                currentLocation = curLatLng;
            }
            return nearbyPlacesInfo;
        }
        else {
            return null;
        }
    }

    /**
     * Loads attractions from location for list view, we need to return Place data type.
     *
     * @param curLatLng users current location
     * @param placeList list of nearby places in Place data type
     * @return Place list that holds nearby places
     */
    public static List<Place> loadAttractionsFromLocationToPlaces(
            LatLng curLatLng,
            List<Place> placeList) {
        placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));
        for (Place place : placeList) {
            String distance = formatDistanceBetween(curLatLng, place.location);
            place.setDistance(distance);
        }
        return placeList;
    }

    /**
     * Loads attractions from location for map view, we need to return BaseMarkerOption data type.
     *
     * @param curLatLng users current location
     * @param placeList list of nearby places in Place data type
     * @return BaseMarkerOptions list that holds nearby places
     */
    public static List<NearbyBaseMarker> loadAttractionsFromLocationToBaseMarkerOptions(
            LatLng curLatLng,
            List<Place> placeList,
            Context context) {
        List<NearbyBaseMarker> baseMarkerOptions = new ArrayList<>();

        if (placeList == null) {
            return baseMarkerOptions;
        }

        placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));

        VectorDrawableCompat vectorDrawable = null;
        try {
            vectorDrawable = VectorDrawableCompat.create(
                    context.getResources(), R.drawable.ic_custom_map_marker, context.getTheme()
            );
        } catch (Resources.NotFoundException e) {
            // ignore when running tests.
        }
        if (vectorDrawable != null) {
            Bitmap icon = UiUtils.getBitmap(vectorDrawable);

            for (Place place : placeList) {
                String distance = formatDistanceBetween(curLatLng, place.location);
                place.setDistance(distance);

                NearbyBaseMarker nearbyBaseMarker = new NearbyBaseMarker();
                nearbyBaseMarker.title(place.name);
                nearbyBaseMarker.position(
                        new com.mapbox.mapboxsdk.geometry.LatLng(
                                place.location.getLatitude(),
                                place.location.getLongitude()));
                nearbyBaseMarker.place(place);
                nearbyBaseMarker.icon(IconFactory.getInstance(context)
                        .fromBitmap(icon));

                baseMarkerOptions.add(nearbyBaseMarker);
            }
        }
        return baseMarkerOptions;
    }

    /**
     * We pass this variable as a group of placeList and boundaryCoordinates
     */
    public class NearbyPlacesInfo {
        public List<Place> placeList; // List of nearby places
        public LatLng[] boundaryCoordinates; // Corners of nearby area
    }
}
