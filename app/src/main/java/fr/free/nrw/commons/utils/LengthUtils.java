package fr.free.nrw.commons.utils;

import java.text.NumberFormat;

import fr.free.nrw.commons.location.LatLng;

public class LengthUtils {
    /** Returns a formatted distance string between two points.
     * @param point1 LatLng type point1
     * @param point2 LatLng type point2
     * @return string distance
     */
    public static String formatDistanceBetween(LatLng point1, LatLng point2) {
        if (point1 == null || point2 == null) {
            return null;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        double distance = Math.round(computeDistanceBetween(point1, point2));

        // Adjust to KM if M goes over 1000 (see javadoc of method for note
        // on only supporting metric)
        if (distance >= 1000) {
            numberFormat.setMaximumFractionDigits(1);
            return numberFormat.format(distance / 1000) + "km";
        }
        return numberFormat.format(distance) + "m";
    }

    /**
     * Computes the distance between two points.
     * @param from one of the two end points
     * @param to one of the two end points
     * @return distance between the points in meter
     */
    public static double computeDistanceBetween(LatLng from, LatLng to) {
        return computeAngleBetween(from, to) * 6371009.0D; // Earth's radius in meter
    }

    /**
     * Computes angle between two points
     *
     * @param from Point A
     * @param to   Point B
     * @return Angle in radius
     */
    private static double computeAngleBetween(LatLng from, LatLng to) {
        return distanceRadians(Math.toRadians(from.getLatitude()),
                Math.toRadians(from.getLongitude()),
                Math.toRadians(to.getLatitude()),
                Math.toRadians(to.getLongitude()));
    }

    /**
     * Computes arc length between 2 points
     * @param lat1 Latitude of point A
     * @param lng1 Longitude of point A
     * @param lat2 Latitude of point B
     * @param lng2 Longitude of point B
     * @return Arc length between the points
     */
    private static double distanceRadians(double lat1, double lng1, double lat2, double lng2) {
        return arcHav(havDistance(lat1, lat2, lng1 - lng2));
    }

    /**
     * Gets twice the amount of arc sine of (x)^(1/2)
     * @param x Angle in radian
     * @return Twice the angle
     */
    private static double arcHav(double x) {
        return 2.0D * Math.asin(Math.sqrt(x));
    }

    /**
     * Computes distance between two points that are on same Longitude
     * @param lat1 Latitude of point A
     * @param lat2 Latitude of point B
     * @param longitude Longitude on which they lie
     * @return Arc length between points
     */
    private static double havDistance(double lat1, double lat2, double longitude) {
        return hav(lat1 - lat2) + hav(longitude) * Math.cos(lat1) * Math.cos(lat2);
    }

    /**
     * Computes square sine function for half of the value entered
     * @param x Angle in radians
     * @return One-forth of square of sine function
     */
    private static double hav(double x) {
        double sinHalf = Math.sin(x * 0.5D);
        return sinHalf * sinHalf;
    }
}
