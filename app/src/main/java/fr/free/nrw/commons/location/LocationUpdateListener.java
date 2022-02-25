package fr.free.nrw.commons.location;

import fr.free.nrw.commons.data.models.location.LatLng;

public interface LocationUpdateListener {
    void onLocationChangedSignificantly(LatLng latLng); // Will be used to update all nearby markers on the map
    void onLocationChangedSlightly(LatLng latLng); // Will be used to track users motion
    void onLocationChangedMedium(LatLng latLng); // Will be used updating nearby card view notification
}
