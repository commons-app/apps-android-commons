package fr.free.nrw.commons.nearby;

import com.mapbox.mapboxsdk.annotations.Marker;

public class MarkerPlaceGroup {
    private Marker marker;
    private boolean isBookmarked;
    private Place place;

    public MarkerPlaceGroup(Marker marker, boolean isBookmarked, Place place) {
        this.marker = marker;
        this.isBookmarked = isBookmarked;
        this.place = place;
    }

    public Marker getMarker() {
        return marker;
    }

    public Place getPlace() {
        return place;
    }

    public boolean getIsBookmarked() {
        return isBookmarked;
    }
}