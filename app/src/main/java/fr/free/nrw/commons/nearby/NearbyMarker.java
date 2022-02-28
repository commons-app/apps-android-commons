package fr.free.nrw.commons.nearby;

import com.mapbox.mapboxsdk.annotations.Marker;
import fr.free.nrw.commons.data.models.nearby.Place;

public class NearbyMarker extends Marker {
    private final Place place;
    private NearbyBaseMarker nearbyBaseMarker;

    /**
     * Creates a instance of {@link Marker} using the builder of Marker.
     *
     * @param baseMarkerOptions The builder used to construct the Marker.
     */
    NearbyMarker(NearbyBaseMarker baseMarkerOptions, Place place) {
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
}
