package fr.free.nrw.commons.nearby;

import com.mapbox.mapboxsdk.annotations.Marker;

public class NearbyMarker extends Marker {
    private Place place;
    private NearbyBaseMarker nearbyBaseMarker;

    /**
     * Creates a instance of {@link Marker} using the builder of Marker.
     *
     * @param baseMarkerOptions The builder used to construct the Marker.
     */
    public NearbyMarker(NearbyBaseMarker baseMarkerOptions, Place place) {
        super(baseMarkerOptions);
        this.place = place;
        this.nearbyBaseMarker = baseMarkerOptions;
    }

    public NearbyBaseMarker getNearbyBaseMarker() {
        return nearbyBaseMarker;
    }

    public Place getPlace() {
        return place;
    }

    public void setNearbyBaseMarker(NearbyBaseMarker nearbyBaseMarker) {
        this.nearbyBaseMarker = nearbyBaseMarker;
    }
}
