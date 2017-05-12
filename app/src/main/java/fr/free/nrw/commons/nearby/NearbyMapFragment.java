package fr.free.nrw.commons.nearby;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.MapboxTelemetry;

import java.util.List;

import fr.free.nrw.commons.R;

public class NearbyMapFragment extends android.support.v4.app.Fragment {
    private NearbyAsyncTask nearbyAsyncTask;

    @BindView(R.id.mapview) MapView mapView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    public NearbyMapFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby_map, container, false);
        ButterKnife.bind(this, view);

        mapView.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
        return view;
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
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected List<BaseMarkerOptions> doInBackground(Void... params) {
            return NearbyController.loadAttractionsFromLocationToBaseMarkerOptions(
                    ((NearbyActivity)getActivity()).getLocationManager().getLatestLocation(), getActivity()
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
