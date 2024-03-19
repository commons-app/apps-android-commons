package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.location.LatLng;
import timber.log.Timber;

public class LocationUtils {
    public static final double RADIUS_OF_EARTH_KM = 6371.0; // Earth's radius in kilometers

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


    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dlon = lon2Rad - lon1Rad;
        double dlat = lat2Rad - lat1Rad;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = RADIUS_OF_EARTH_KM * c;

        return distance;
    }
}
