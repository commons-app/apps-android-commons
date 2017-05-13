package fr.free.nrw.commons.nearby;

import android.graphics.Bitmap;
import android.net.Uri;

import fr.free.nrw.commons.location.LatLng;

public class Place {

    public final String name;
    public final String description;
    public final String longDescription;
    public final Uri secondaryImageUrl;
    public final LatLng location;

    public Bitmap image;
    public Bitmap secondaryImage;
    public String distance;


    public Place(String name, String description, String longDescription,
                 Uri secondaryImageUrl, LatLng location) {
        this.name = name;
        this.description = description;
        this.longDescription = longDescription;
        this.secondaryImageUrl = secondaryImageUrl;
        this.location = location;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Place) {
            Place that = (Place)o;
            return this.name.equals(that.name) && this.location.equals(that.location);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 31 + this.location.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Place(%s@%s)", name, location);
    }

}
