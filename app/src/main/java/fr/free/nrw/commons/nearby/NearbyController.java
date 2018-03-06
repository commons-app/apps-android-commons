package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.graphics.drawable.VectorDrawableCompat;

import com.mapbox.mapboxsdk.annotations.IconFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.UiUtils;
import timber.log.Timber;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

public class NearbyController {
    private static final int MAX_RESULTS = 1000;
    private final NearbyPlaces nearbyPlaces;
    private final SharedPreferences prefs;

    @Inject
    public NearbyController(NearbyPlaces nearbyPlaces,
                            @Named("default_preferences") SharedPreferences prefs) {
        this.nearbyPlaces = nearbyPlaces;
        this.prefs = prefs;
    }

    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param curLatLng current location for user
     * @return NearbyPlacesInfo a variable holds Place list without distance information
     * and boundary coordinates of current Place List
     */
    public NearbyPlacesInfo loadAttractionsFromLocation(LatLng curLatLng) {

        Timber.d("Loading attractions near %s", curLatLng);
        NearbyPlacesInfo nearbyPlacesInfo = new NearbyPlacesInfo();

        if (curLatLng == null) {
            return null;
        }
        List<Place> places = nearbyPlaces.getFromWikidataQuery(curLatLng, Locale.getDefault().getLanguage());

        LatLng[] boundaryCoordinates = {places.get(0).location,   // south
                                        places.get(0).location, // north
                                        places.get(0).location, // west
                                        places.get(0).location};// east, init with a random location

        if (curLatLng != null) {
            Timber.d("Sorting places by distance...");
            final Map<Place, Double> distances = new HashMap<>();
            for (Place place: places) {
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
        return nearbyPlacesInfo;
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

        VectorDrawableCompat vectorDrawable = VectorDrawableCompat.create(
                context.getResources(), R.drawable.ic_custom_map_marker, context.getTheme()
        );
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

    public class NearbyPlacesInfo {
        List<Place> placeList; // List of nearby places
        LatLng[] boundaryCoordinates; // Corners of nearby area
    }
}
