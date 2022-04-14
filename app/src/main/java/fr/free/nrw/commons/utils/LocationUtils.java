package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.location.LatLng;
import timber.log.Timber;

public class LocationUtils {
    public static LatLng mapBoxLatLngToCommonsLatLng(com.mapbox.mapboxsdk.geometry.LatLng mapBoxLatLng) {
        return new LatLng(mapBoxLatLng.getLatitude(), mapBoxLatLng.getLongitude(), 0);
    }

    public static com.mapbox.mapboxsdk.geometry.LatLng commonsLatLngToMapBoxLatLng(LatLng commonsLatLng) {
        return new com.mapbox.mapboxsdk.geometry.LatLng(commonsLatLng.getLatitude(), commonsLatLng.getLongitude());
    }

    public static LatLng deriveUpdatedLocationFromSearchQuery(String customQuery) {
        LatLng latLng = null;
        final int indexOfPrefix = customQuery.indexOf("Point(");
        if (indexOfPrefix == -1) {
            Timber.e("Invalid prefix index - Seems like user has entered an invalid query");
            return latLng;
        }
        final int indexOfSuffix = customQuery.indexOf(")\"", indexOfPrefix);
        if (indexOfSuffix == -1) {
            Timber.e("Invalid suffix index - Seems like user has entered an invalid query");
            return latLng;
        }
        String latLngString = customQuery.substring(indexOfPrefix+"Point(".length(), indexOfSuffix);
        if (latLngString.isEmpty()) {
            return null;
        }

        String latLngArray[] = latLngString.split(" ");
        if (latLngArray.length != 2) {
            return null;
        }

        try {
            latLng = new LatLng(Double.parseDouble(latLngArray[1].trim()),
                Double.parseDouble(latLngArray[0].trim()), 1f);
        }catch (Exception e){
            Timber.e("Error while parsing user entered lat long: %s", e);
        }

        return latLng;
    }
}
