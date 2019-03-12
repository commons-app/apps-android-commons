package fr.free.nrw.commons.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static LatLng latLngFromPointString(String pointString) {
        double latitude;
        double longitude;
        Matcher matcher = Pattern.compile("Point\\(([^ ]+) ([^ ]+)\\)").matcher(pointString);
        if (!matcher.find()) {
            return null;
        }
        try {
            longitude = Double.parseDouble(matcher.group(1));
            latitude = Double.parseDouble(matcher.group(2));
        } catch (NumberFormatException e) {
            return null;
        }

        return new LatLng(latitude, longitude, 0);
    }
}
