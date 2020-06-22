package fr.free.nrw.commons.nearby;

import com.mapbox.mapboxsdk.annotations.Marker;

public class NearbyMarker extends Marker {

    private NearbyBaseMarker nearbyBaseMarker;

    /**
     * Creates a instance of {@link Marker} using the builder of Marker.
     *
     * @param baseMarkerOptions The builder used to construct the Marker.
     */
    NearbyMarker(NearbyBaseMarker baseMarkerOptions) {
        super(baseMarkerOptions);
        this.nearbyBaseMarker = baseMarkerOptions;
    }

    public NearbyBaseMarker getNearbyBaseMarker() {
        return nearbyBaseMarker;
    }

}
