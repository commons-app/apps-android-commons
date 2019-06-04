package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

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
    public static double currentLocationSearchRadius = 10.0; //in kilometers
    public static LatLng currentLocation; // Users latest fetched location
    public static LatLng latestSearchLocation; // Can be current and camera target on search this area button is used
    public static double latestSearchRadius = 10.0; // Any last search radius except closest result search

    @Inject
    public NearbyController(NearbyPlaces nearbyPlaces) {
        this.nearbyPlaces = nearbyPlaces;
    }


    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param curLatLng current location for user
     * @param searchLatLng the location user wants to search around
     * @param returnClosestResult if this search is done to find closest result or all results
     * @return NearbyPlacesInfo a variable holds Place list without distance information
     * and boundary coordinates of current Place List
     */
    public NearbyPlacesInfo loadAttractionsFromLocation(LatLng curLatLng, LatLng searchLatLng, boolean returnClosestResult, boolean checkingAroundCurrentLocation) throws IOException {

        Timber.d("Loading attractions near %s", searchLatLng);
        NearbyPlacesInfo nearbyPlacesInfo = new NearbyPlacesInfo();

        if (searchLatLng == null) {
            Timber.d("Loading attractions nearby, but curLatLng is null");
            return null;
        }
        List<Place> places = nearbyPlaces.radiusExpander(searchLatLng, Locale.getDefault().getLanguage(), returnClosestResult);

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
            nearbyPlacesInfo.curLatLng = curLatLng;
            nearbyPlacesInfo.searchLatLng = searchLatLng;
            nearbyPlacesInfo.placeList = places;
            nearbyPlacesInfo.boundaryCoordinates = boundaryCoordinates;

            if (!returnClosestResult) {
                latestSearchLocation = searchLatLng;
                latestSearchRadius = nearbyPlaces.radius*1000; // to meter

                if (checkingAroundCurrentLocation) {
                    currentLocationSearchRadius = nearbyPlaces.radius*1000; // to meter
                    currentLocation = curLatLng;
                }
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
    static List<Place> loadAttractionsFromLocationToPlaces(
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
            Context context,
            List<Place> bookmarkplacelist) {
        List<NearbyBaseMarker> baseMarkerOptions = new ArrayList<>();

        if (placeList == null) {
            return baseMarkerOptions;
        }

        placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));

        VectorDrawableCompat vectorDrawable = null;
        try {
            vectorDrawable = VectorDrawableCompat.create(
                    context.getResources(), R.drawable.ic_custom_bookmark_marker, context.getTheme()
            );
        } catch (Resources.NotFoundException e) {
            // ignore when running tests.
        }
        if (vectorDrawable != null) {
            Bitmap icon = UiUtils.getBitmap(vectorDrawable);

            for (Place place : bookmarkplacelist) {

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
                placeList.remove(place);

                baseMarkerOptions.add(nearbyBaseMarker);
            }
        }

        vectorDrawable = null;
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
        public LatLng curLatLng; // current location when this places are populated
        public LatLng searchLatLng; //search location for finding this places
    }
}
