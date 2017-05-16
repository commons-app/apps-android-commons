package fr.free.nrw.commons.nearby;

import android.content.Context;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.CommonsAppSharedPref;
import timber.log.Timber;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;


public class NearbyController {
    private static final int MAX_RESULTS = 1000;

    /**
     * Prepares Place list to make their distance information update later.
     * @param curLatLng current location for user
     * @param context context
     * @return Place list without distance information
     */
    public static List<Place> loadAttractionsFromLocation(LatLng curLatLng, Context context) {
        Timber.d("Loading attractions near %s", curLatLng);
        if (curLatLng == null) {
            return Collections.emptyList();
        }
        List<Place> places = CommonsAppSharedPref.getInstance(context)
                .getPreferenceBoolean("useWikidata", true)
                ? NearbyPlaces.getInstance().getFromWikidataQuery(
                context,
                curLatLng,
                Locale.getDefault().getLanguage())
                : NearbyPlaces.getInstance().getFromWikiNeedsPictures();
        if (curLatLng != null) {
            Timber.d("Sorting places by distance...");
            final Map<Place, Double> distances = new HashMap<>();
            for (Place place: places) {
                distances.put(place, computeDistanceBetween(place.location, curLatLng));
            }
            Collections.sort(places,
                    new Comparator<Place>() {
                        @Override
                        public int compare(Place lhs, Place rhs) {
                            double lhsDistance = distances.get(lhs);
                            double rhsDistance = distances.get(rhs);
                            return (int) (lhsDistance - rhsDistance);
                        }
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
     * @return BaseMarkerOprions list that holds nearby places
     */
    public static List<NearbyBaseMarker> loadAttractionsFromLocationToBaseMarkerOptions(
            LatLng curLatLng,
            List<Place> placeList,
            Context context) {
        List<NearbyBaseMarker> baseMarkerOptionses = new ArrayList<>();
        placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));
        for (Place place: placeList) {
            String distance = formatDistanceBetween(curLatLng, place.location);
            place.setDistance(distance);

            Icon icon = IconFactory.getInstance(context)
                    .fromResource(R.drawable.custom_map_marker);

            NearbyBaseMarker nearbyBaseMarker = new NearbyBaseMarker();
            nearbyBaseMarker.title(place.name);
            nearbyBaseMarker.position(
                    new com.mapbox.mapboxsdk.geometry.LatLng(
                            place.location.latitude,
                            place.location.longitude));
            nearbyBaseMarker.place(place);
            nearbyBaseMarker.icon(icon);

            baseMarkerOptionses.add(nearbyBaseMarker);
        }
        return baseMarkerOptionses;
    }
}
