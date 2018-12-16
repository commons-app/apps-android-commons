package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.location.LatLng;

public class PlaceUtils {

    /**
     * Converts our defined LatLng to string, to put as String
     * @param latLng latlang will be converted to string
     * @return latitude + "/" + longitude
     */
    public static String latLangToString(LatLng latLng) {
        return latLng.getLatitude()+"/"+latLng.getLongitude();
    }

    /**
     * Converts latitude + "/" + longitude string to commons LatLng
     * @param latLngString latitude + "/" + longitude string
     * @return commons LatLng
     */
    public static LatLng stringToLatLng(String latLngString) {
        String[] parts = latLngString.split("/");
        return new LatLng(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), 0);
    }
}
