package fr.free.nrw.commons.location;

public interface LocationUpdateListener {
    void onLocationChangedSignificantly(LatLng latLng);
    void onLocationChangedSlightly(LatLng latLng);
}
