package fr.free.nrw.commons.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.free.nrw.commons.location.LatLng;

public class PlaceUtils {

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
