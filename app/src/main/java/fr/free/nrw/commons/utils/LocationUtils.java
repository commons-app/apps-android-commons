package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.location.LatLng;

public class LocationUtils {

  public static LatLng mapBoxLatLngToCommonsLatLng(
      com.mapbox.mapboxsdk.geometry.LatLng mapBoxLatLng) {
    return new LatLng(mapBoxLatLng.getLatitude(), mapBoxLatLng.getLongitude(), 0);
  }

  public static com.mapbox.mapboxsdk.geometry.LatLng commonsLatLngToMapBoxLatLng(
      LatLng commonsLatLng) {
    return new com.mapbox.mapboxsdk.geometry.LatLng(commonsLatLng.getLatitude(),
        commonsLatLng.getLongitude());
  }
}
