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
     * @param curLatLng current location for user
     * @return Place list without distance information
     */
    public List<Place> loadAttractionsFromLocation(LatLng curLatLng) {
        Timber.d("Loading attractions near %s", curLatLng);
        if (curLatLng == null) {
            return Collections.emptyList();
        }
        List<Place> places = nearbyPlaces.getFromWikidataQuery(curLatLng, Locale.getDefault().getLanguage());
        if (curLatLng != null) {
            Timber.d("Sorting places by distance...");
            final Map<Place, Double> distances = new HashMap<>();
            for (Place place: places) {
                distances.put(place, computeDistanceBetween(place.location, curLatLng));
            }
            Collections.sort(places,
                    (lhs, rhs) -> {
                        double lhsDistance = distances.get(lhs);
                        double rhsDistance = distances.get(rhs);
                        return (int) (lhsDistance - rhsDistance);
                    }
            );
        }
        return places;
    }

    /**
     * Loads attractions from location for list view, we need to return Place data type.
     * @param curLatLng users current location
     * @param placeList list of nearby places in Place data type
     * @return Place list that holds nearby places
     */
    public static List<Place> loadAttractionsFromLocationToPlaces(
            LatLng curLatLng,
            List<Place> placeList) {
        placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));
        for (Place place: placeList) {
            String distance = formatDistanceBetween(curLatLng, place.location);
            place.setDistance(distance);
        }
        return placeList;
    }

    /**
     *Loads attractions from location for map view, we need to return BaseMarkerOption data type.
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

        Bitmap icon = UiUtils.getBitmap(
                VectorDrawableCompat.create(
                        context.getResources(), R.drawable.ic_custom_map_marker, context.getTheme()
                ));

        for (Place place: placeList) {
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
        return baseMarkerOptions;
    }
}
