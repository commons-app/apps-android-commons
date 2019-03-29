package fr.free.nrw.commons.location;

import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * a latitude and longitude point with accuracy information, often of a picture
 */
public class LatLng implements Parcelable {

    private final double latitude;
    private final double longitude;
    private final float accuracy;

    /**
     * Accepts latitude and longitude.
     * North and South values are cut off at 90°
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @param accuracy the accuracy
     *
     * Examples:
     * the Statue of Liberty is located at 40.69° N, 74.04° W
     * The Statue of Liberty could be constructed as LatLng(40.69, -74.04, 1.0)
     * where positive signifies north, east and negative signifies south, west.
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

    public LatLng(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        accuracy = in.readFloat();
    }

    /**
     * gets the latitude and longitude of a given non-null location
     * @param location the non-null location of the user
     * @return LatLng the Latitude and Longitude of a given location
     */
    public static LatLng from(@NonNull Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude(), location.getAccuracy());
    }

    /**
     * creates a hash code for the longitude and longitude
     */
    public int hashCode() {
        byte var1 = 1;
        long var2 = Double.doubleToLongBits(this.latitude);
        int var3 = 31 * var1 + (int)(var2 ^ var2 >>> 32);
        var2 = Double.doubleToLongBits(this.longitude);
        var3 = 31 * var3 + (int)(var2 ^ var2 >>> 32);
        return var3;
    }

    /**
     * checks for equality of two LatLng objects
     * @param o the second LatLng object
     */
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

    /**
     * returns a string representation of the latitude and longitude
     */
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

    public Uri getGmmIntentUri() {
        return Uri.parse("geo:0,0?q=" + latitude + "," + longitude);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeFloat(accuracy);
    }

    public static final Creator<LatLng> CREATOR = new Creator<LatLng>() {
        @Override
        public LatLng createFromParcel(Parcel in) {
            return new LatLng(in);
        }

        @Override
        public LatLng[] newArray(int size) {
            return new LatLng[size];
        }
    };
}

