package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.location.LocationManager;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.Place;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Date;
import timber.log.Timber;

public class MapUtils {
    public static final float ZOOM_LEVEL = 14f;
    private static final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005;
    private static final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004;
    public static final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final float ZOOM_OUT = 0f;

    public static final LatLng defaultLatLng = new fr.free.nrw.commons.location.LatLng(51.50550,-0.07520,1f);

    public static void centerMapToPlace(Place placeToCenter, MapboxMap mapBox, Place lastPlaceToCenter, Context context) {
        Timber.d("Map is centered to place");
        final double cameraShift;
        if(null != placeToCenter){
            lastPlaceToCenter = placeToCenter;
        }
        if (null != lastPlaceToCenter) {
            final Configuration configuration = context.getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT;
            } else {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE;
            }
            final CameraPosition position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(
                    new fr.free.nrw.commons.location.LatLng(lastPlaceToCenter.location.getLatitude() - cameraShift,
                        lastPlaceToCenter.getLocation().getLongitude(),
                        0))) // Sets the new camera position
                .zoom(ZOOM_LEVEL) // Same zoom level
                .build();
            mapBox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
        }
    }

    public static void centerMapToDefaultLatLng(MapboxMap mapBox) {
        final CameraPosition position = new CameraPosition.Builder()
            .target(LocationUtils.commonsLatLngToMapBoxLatLng(defaultLatLng))
            .zoom(MapUtils.ZOOM_OUT)
            .build();
        if(mapBox != null){
            mapBox.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
    }

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
