package fr.free.nrw.commons.nearby;

import android.graphics.Bitmap;
import android.net.Uri;

public class Place {

    public String name;
    public String description;
    public String longDescription;
    public Uri imageUrl;
    public Uri secondaryImageUrl;
    public LatLng location;

    public Bitmap image;
    public Bitmap secondaryImage;
    public String distance;

    public Place() {}

    public Place(String name, String description, String longDescription, Uri imageUrl,
                 Uri secondaryImageUrl, LatLng location) {
        this.name = name;
        this.description = description;
        this.longDescription = longDescription;
        this.imageUrl = imageUrl;
        this.secondaryImageUrl = secondaryImageUrl;
        this.location = location;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

}
