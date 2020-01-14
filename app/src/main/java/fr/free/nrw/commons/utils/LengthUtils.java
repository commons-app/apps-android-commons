package fr.free.nrw.commons.utils;

import androidx.annotation.NonNull;

import java.text.NumberFormat;

import fr.free.nrw.commons.location.LatLng;

public class LengthUtils {
    /**
     * Returns a formatted distance string between two points.
     *
     * @param point1 LatLng type point1
     * @param point2 LatLng type point2
     * @return string distance
     */
    public static String formatDistanceBetween(LatLng point1, LatLng point2) {
        if (point1 == null || point2 == null) {
            return null;
        }

        int distance = (int) Math.round(computeDistanceBetween(point1, point2));
        return formatDistance(distance);
    }

    /**
     * Format a distance (in meters) as a string
     * Example: 140 -> "140m"
     * 3841 -> "3.8km"
     *
     * @param distance Distance, in meters
     * @return A string representing the distance
     * @throws IllegalArgumentException If distance is negative
     */
    public static String formatDistance(int distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance must be non-negative");
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();

        // Adjust to km if distance is over 1000m (1km)
        if (distance >= 1000) {
            numberFormat.setMaximumFractionDigits(1);
            return numberFormat.format(distance / 1000.0) + "km";
        }

        // Otherwise just return in meters
        return numberFormat.format(distance) + "m";
    }

    /**
     * Computes the distance between two points.
     *
     * @param point1 LatLng type point1
     * @param point2 LatLng type point2
     * @return distance between the points in meters
     * @throws NullPointerException if one or both the points are null
     */
    public static double computeDistanceBetween(@NonNull LatLng point1, @NonNull LatLng point2) {
        return computeAngleBetween(point1, point2) * 6371009.0D; // Earth's radius in meter
    }

    /**
     * Computes angle between two points
     *
     * @param point1 one of the two end points
     * @param point2 one of the two end points
     * @return Angle in radius
     * @throws NullPointerException if one or both the points are null
     */
    private static double computeAngleBetween(@NonNull LatLng point1, @NonNull LatLng point2) {
        return distanceRadians(
                Math.toRadians(point1.getLatitude()),
                Math.toRadians(point1.getLongitude()),
                Math.toRadians(point2.getLatitude()),
                Math.toRadians(point2.getLongitude())
        );
    }

    /**
     * Computes arc length between 2 points
     *
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
     * Computes inverse of haversine
     *
     * @param x Angle in radian
     * @return Inverse of haversine
     */
    private static double arcHav(double x) {
        return 2.0D * Math.asin(Math.sqrt(x));
    }

    /**
     * Computes distance between two points that are on same Longitude
     *
     * @param lat1      Latitude of point A
     * @param lat2      Latitude of point B
     * @param longitude Longitude on which they lie
     * @return Arc length between points
     */
    private static double havDistance(double lat1, double lat2, double longitude) {
        return hav(lat1 - lat2) + hav(longitude) * Math.cos(lat1) * Math.cos(lat2);
    }

    /**
     * Computes haversine
     *
     * @param x Angle in radians
     * @return Haversine of x
     */
    private static double hav(double x) {
        double sinHalf = Math.sin(x * 0.5D);
        return sinHalf * sinHalf;
    }
}
