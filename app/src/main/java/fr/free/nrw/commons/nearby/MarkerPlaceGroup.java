package fr.free.nrw.commons.nearby;

/**
 * This class groups visual map item Marker with the reated data of displayed place and information
 * of bookmark
 */
public class MarkerPlaceGroup {
    private boolean isBookmarked; // True if user bookmarked the place
    private Place place; // Place of the location displayed by the marker

    public MarkerPlaceGroup(boolean isBookmarked, Place place) {
        this.isBookmarked = isBookmarked;
        this.place = place;
    }

    public Place getPlace() {
        return place;
    }

    public boolean getIsBookmarked() {
        return isBookmarked;
    }
}