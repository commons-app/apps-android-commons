package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.MapboxTelemetry;

import java.lang.reflect.Type;
import java.util.List;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.CommonsAppSharedPref;
import fr.free.nrw.commons.utils.UriDeserializer;

public class NearbyMapFragment extends android.support.v4.app.Fragment {
    private MapView mapView;
    private Gson gson;
    private List<Place> placeList;
    private List<NearbyBaseMarker> baseMarkerOptionses;
    private fr.free.nrw.commons.location.LatLng curLatLng;

    public NearbyMapFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriDeserializer())
                .create();
        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList");
            String gsonLatLng = bundle.getString("CurLatLng");
            Type listType = new TypeToken<List<Place>>() {}.getType();
            placeList = gson.fromJson(gsonPlaceList, listType);
            Type curLatLngType = new TypeToken<fr.free.nrw.commons.location.LatLng>() {}.getType();
            curLatLng = gson.fromJson(gsonLatLng, curLatLngType);
            baseMarkerOptionses = NearbyController
                    .loadAttractionsFromLocationToBaseMarkerOptions(curLatLng,
                            placeList,
                            getActivity());
        }
        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (curLatLng != null) {
            setupMapView(savedInstanceState);
        }

        setHasOptionsMenu(false);

        return mapView;
    }

    private void setupMapView(Bundle savedInstanceState) {
        MapboxMapOptions options = new MapboxMapOptions()
                .styleUrl(Style.OUTDOORS)
                .camera(new CameraPosition.Builder()
                        .target(new LatLng(curLatLng.latitude, curLatLng.longitude))
                        .zoom(11)
                        .build());

        // create map
        mapView = new MapView(getActivity(), options);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.addMarkers(baseMarkerOptionses);

                mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        if (marker instanceof NearbyMarker) {
                            NearbyMarker nearbyMarker = (NearbyMarker) marker;
                            Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
                            NearbyInfoDialog.showYourself(getActivity(), place);
                        }
                        return false;
                    }
                });
            }
        });
        if (CommonsAppSharedPref.getInstance(getActivity()).getPreferenceBoolean("theme",true)) {
            mapView.setStyleUrl(getResources().getString(R.string.map_theme_dark));
        } else {
            mapView.setStyleUrl(getResources().getString(R.string.map_theme_light));
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        if (mapView != null) {
            mapView.onStart();
        }
        super.onStart();
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mapView != null) {
            mapView.onResume();
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroyView();
    }
}
