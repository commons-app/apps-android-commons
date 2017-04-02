package fr.free.nrw.commons.location;

public class LatLng {

    public final double latitude;
    public final double longitude;

    /**
     * Accepts latitude and longitude
     * @param latitude
     * @param longitude
     */
    public LatLng(double latitude, double longitude) {
        if(-180.0D <= longitude && longitude < 180.0D) {
            this.longitude = longitude;
        } else {
            this.longitude = ((longitude - 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D;
        }
        this.latitude = Math.max(-90.0D, Math.min(90.0D, latitude));
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
        if(this == o) {
            return true;
        } else if(!(o instanceof LatLng)) {
            return false;
        } else {
            LatLng var2 = (LatLng)o;
            return Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(var2.latitude) && Double.doubleToLongBits(this.longitude) == Double.doubleToLongBits(var2.longitude);
        }
    }

    public String toString() {
        return "lat/lng: (" + this.latitude + "," + this.longitude + ")";
    }
}
