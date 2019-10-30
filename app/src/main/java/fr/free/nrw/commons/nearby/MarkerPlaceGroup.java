package fr.free.nrw.commons.nearby;

import com.mapbox.mapboxsdk.annotations.Marker;

/**
 * This class groups visual map item Marker with the reated data of displayed place and information
 * of bookmark
 */
public class MarkerPlaceGroup {
    private Marker marker; // Marker item from the map
    private boolean isBookmarked; // True if user bookmarked the place
    private Place place; // Place of the location displayed by the marker

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