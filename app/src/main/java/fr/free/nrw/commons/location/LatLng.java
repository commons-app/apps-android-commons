package fr.free.nrw.commons.location;

public class LatLng {

    private final double latitude;
    private final double longitude;
    private final float accuracy;

    /** Accepts latitude and longitude.
     * North and South values are cut off at 90°
     *
     * @param latitude double value
     * @param longitude double value
     */
    public LatLng(double latitude, double longitude, float accuracy) {
        if (-180.0D <= longitude && longitude < 180.0D) {
            this.longitude = longitude;
        } else {
            this.longitude = ((longitude - 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D;
        }
        this.latitude = Math.max(-90.0D, Math.min(90.0D, latitude));
        this.accuracy = accuracy;
    }

    public int hashCode() {
        boolean var1 = true;
        byte var2 = 1;
        long var3 = Double.doubleToLongBits(this.latitude);
        int var5 = 31 * var2 + (int)(var3 ^ var3 >>> 32);
        var3 = Double.doubleToLongBits(this.longitude);
        var5 = 31 * var5 + (int)(var3 ^ var3 >>> 32);
        return var5;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof LatLng)) {
            return false;
        } else {
            LatLng var2 = (LatLng)o;
            return Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(var2.latitude) && Double.doubleToLongBits(this.longitude) == Double.doubleToLongBits(var2.longitude);
        }
    }

    public String toString() {
        return "lat/lng: (" + this.latitude + "," + this.longitude + ")";
    }

    /**
     * Rounds the float to 4 digits and returns absolute value.
     *
     * @param coordinate A coordinate value as string.
     * @return String of the rounded number.
     */
    private String formatCoordinate(double coordinate) {
        double roundedNumber = Math.round(coordinate * 10000d) / 10000d;
        double absoluteNumber = Math.abs(roundedNumber);
        return String.valueOf(absoluteNumber);
    }

    /**
     * Returns "N" or "S" depending on the latitude.
     *
     * @return "N" or "S".
     */
    private String getNorthSouth() {
        if (this.latitude < 0) {
            return "S";
        }

        return "N";
    }

    /**
     * Returns "E" or "W" depending on the longitude.
     *
     * @return "E" or "W".
     */
    private String getEastWest() {
        if (this.longitude >= 0 && this.longitude < 180) {
            return "E";
        }

        return "W";
    }

    /**
     * Returns a nicely formatted coordinate string. Used e.g. in
     * the detail view.
     *
     * @return The formatted string.
     */
    public String getPrettyCoordinateString() {
        return formatCoordinate(this.latitude) + " " + this.getNorthSouth() + ", "
               + formatCoordinate(this.longitude) + " " + this.getEastWest();
    }

    /**
     * Return the location accuracy in meter.
     *
     * @return float
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * Return the longitude in degrees.
     *
     * @return double
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Return the latitude in degrees.
     *
     * @return double
     */
    public double getLatitude() {
        return latitude;
    }
}
