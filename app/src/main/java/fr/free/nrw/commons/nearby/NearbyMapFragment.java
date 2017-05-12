package fr.free.nrw.commons.nearby;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.MapboxTelemetry;

import java.util.List;

import fr.free.nrw.commons.R;

public class NearbyMapFragment extends android.support.v4.app.Fragment {
    private NearbyAsyncTask nearbyAsyncTask;
    private fr.free.nrw.commons.location.LatLng currentLocation;
    private MapView mapView;

    public NearbyMapFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLocation = ((NearbyActivity)getActivity()).getLocationManager().getLatestLocation();
        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MapboxMapOptions options = new MapboxMapOptions()
                .styleUrl(Style.OUTDOORS)
                .camera(new CameraPosition.Builder()
                        .target(new LatLng(currentLocation.latitude, currentLocation.longitude))
                        .zoom(11)
                        .build());

        // create map
        mapView = new MapView(getActivity(), options);
        mapView.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        return mapView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nearbyAsyncTask = new NearbyAsyncTask();
        nearbyAsyncTask.execute();
    }

    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mapView.onDestroy();
        super.onDestroyView();
    }

    private class NearbyAsyncTask extends AsyncTask<Void, Integer, List<BaseMarkerOptions>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected List<BaseMarkerOptions> doInBackground(Void... params) {
            return NearbyController.loadAttractionsFromLocationToBaseMarkerOptions(currentLocation, getActivity()
            );
        }

        @Override
        protected void onPostExecute(final List<BaseMarkerOptions> baseMarkerOptionses) {
            super.onPostExecute(baseMarkerOptionses);

            if (isCancelled()) {
                return;
            }
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(MapboxMap mapboxMap) {
                    mapboxMap.addMarkers(baseMarkerOptionses);
                }
            });
        }
    }
}
