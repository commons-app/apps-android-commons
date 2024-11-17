package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import timber.log.Timber;

public class MapUtils {
    public static final float ZOOM_LEVEL = 14f;
    public static final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005;
    public static final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004;
    public static final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final float ZOOM_OUT = 0f;

    public static final LatLng defaultLatLng = new fr.free.nrw.commons.location.LatLng(51.50550,-0.07520,1f);

    public static void registerUnregisterLocationListener(final boolean removeLocationListener, LocationServiceManager locationManager, LocationUpdateListener locationUpdateListener) {
        try {
            if (removeLocationListener) {
                locationManager.unregisterLocationManager();
                locationManager.removeLocationListener(locationUpdateListener);
                Timber.d("Location service manager unregistered and removed");
            } else {
                locationManager.addLocationListener(locationUpdateListener);
                locationManager.registerLocationManager();
                Timber.d("Location service manager added and registered");
            }
        }catch (final Exception e){
            Timber.e(e);
            //Broadcasts are tricky, should be catchedonR
        }
    }
}
