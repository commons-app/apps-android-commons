package fr.free.nrw.commons.nearby;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.MapboxTelemetry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.UriDeserializer;

public class NearbyMapFragment extends android.support.v4.app.Fragment {

    private MapView mapView;
    private List<NearbyBaseMarker> baseMarkerOptions;
    private fr.free.nrw.commons.location.LatLng curLatLng;
    public fr.free.nrw.commons.location.LatLng[] boundaryCoordinates;

    private View bottomSheetList;
    private View bottomSheetDetails;

    private BottomSheetBehavior bottomSheetListBehavior;
    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private LinearLayout wikipediaButton;
    private LinearLayout wikidataButton;
    private LinearLayout directionsButton;
    private LinearLayout commonsButton;
    private FloatingActionButton fabPlus;
    private FloatingActionButton fabCamera;
    private FloatingActionButton fabGallery;
    private View transparentView;
    private TextView description;
    private TextView title;
    private TextView distance;
    private ImageView icon;

    private TextView wikipediaButtonText;
    private TextView wikidataButtonText;
    private TextView commonsButtonText;
    private TextView directionsButtonText;

    private boolean isFabOpen=false;
    private Animation rotate_backward;
    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;

    private Place place;
    private Marker selected;
    private Marker currentLocationMarker;
    private MapboxMap mapboxMap;
    private PolygonOptions currentLocationPolygonOptions;

    public NearbyMapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        initViews();
        setListeners();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriDeserializer())
                .create();
        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList");
            String gsonLatLng = bundle.getString("CurLatLng");
            String gsonBoundaryCoordinates = bundle.getString("BoundaryCoord");
            Type listType = new TypeToken<List<Place>>() {}.getType();
            List<Place> placeList = gson.fromJson(gsonPlaceList, listType);
            Type curLatLngType = new TypeToken<fr.free.nrw.commons.location.LatLng>() {}.getType();
            Type gsonBoundaryCoordinatesType = new TypeToken<fr.free.nrw.commons.location.LatLng[]>() {}.getType();
            curLatLng = gson.fromJson(gsonLatLng, curLatLngType);
            baseMarkerOptions = NearbyController
                    .loadAttractionsFromLocationToBaseMarkerOptions(curLatLng,
                            placeList,
                            getActivity());
            boundaryCoordinates = gson.fromJson(gsonBoundaryCoordinates, gsonBoundaryCoordinatesType);
            Log.d("deneme", boundaryCoordinates[0].getLatitude()+","+ boundaryCoordinates[0].getLongitude());
            Log.d("deneme", boundaryCoordinates[1].getLatitude()+","+ boundaryCoordinates[1].getLongitude());
            Log.d("deneme", boundaryCoordinates[2].getLatitude()+","+ boundaryCoordinates[2].getLongitude());
            Log.d("deneme", boundaryCoordinates[3].getLatitude()+","+ boundaryCoordinates[3].getLongitude());
        }
        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);
        setRetainInstance(true);
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

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.getView().setFocusableInTouchMode(true);
        this.getView().requestFocus();
        this.getView().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if(bottomSheetDetailsBehavior.getState() == BottomSheetBehavior
                        .STATE_EXPANDED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return true;
                }
                else if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior
                        .STATE_COLLAPSED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    mapView.getMapAsync(MapboxMap::deselectMarkers);
                    selected=null;
                    return true;
                }
            }
            return false;
        });
    }

    public void updateMapSlightly() {
    // Get arguments from bundle for new location
        Bundle bundle = this.getArguments();
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Uri.class, new UriDeserializer())
            .create();
        if (bundle != null) {
            String gsonLatLng = bundle.getString("CurLatLng");
            Type curLatLngType = new TypeToken<fr.free.nrw.commons.location.LatLng>() {}.getType();
            curLatLng = gson.fromJson(gsonLatLng, curLatLngType);
        }

        updateMapToTrackPosition();
    }

    public void updateMapSignificantly() {
        Bundle bundle = this.getArguments();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriDeserializer())
                .create();
        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList");
            String gsonLatLng = bundle.getString("CurLatLng");
            String gsonBoundaryCoordinates = bundle.getString("BoundaryCoord");
            Type listType = new TypeToken<List<Place>>() {}.getType();
            List<Place> placeList = gson.fromJson(gsonPlaceList, listType);
            Type curLatLngType = new TypeToken<fr.free.nrw.commons.location.LatLng>() {}.getType();
            Type gsonBoundaryCoordinatesType = new TypeToken<fr.free.nrw.commons.location.LatLng[]>() {}.getType();
            curLatLng = gson.fromJson(gsonLatLng, curLatLngType);
            baseMarkerOptions = NearbyController
                    .loadAttractionsFromLocationToBaseMarkerOptions(curLatLng,
                            placeList,
                            getActivity());
            boundaryCoordinates = gson.fromJson(gsonBoundaryCoordinates, gsonBoundaryCoordinatesType);
        }
        updateMapToTrackPosition();
        //addNearbyMarkerstoMapBoxMap();
    }

    // Only update current position marker and camera view
    private void updateMapToTrackPosition() {
        // Change
        Log.d("deneme","updateMapToTrackPosition");
        if (currentLocationMarker != null && mapboxMap != null) {
            LatLng curMapBoxLatLng = new LatLng(curLatLng.getLatitude(),curLatLng.getLongitude());
            ValueAnimator markerAnimator = ObjectAnimator.ofObject(currentLocationMarker, "position",
                    new LatLngEvaluator(), currentLocationMarker.getPosition(),
                    curMapBoxLatLng);
            markerAnimator.setDuration(1000);
            markerAnimator.start();

            List<LatLng> circle = createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                    curLatLng.getAccuracy() * 2, 100);
            if (currentLocationPolygonOptions != null){
                mapboxMap.removePolygon(currentLocationPolygonOptions.getPolygon());
                currentLocationPolygonOptions = new PolygonOptions()
                        .addAll(circle)
                        .strokeColor(Color.parseColor("#55000000"))
                        .fillColor(Color.parseColor("#11000000"));
                mapboxMap.addPolygon(currentLocationPolygonOptions);
            }

            // Make camera to follow user on location change
            CameraPosition position = new CameraPosition.Builder()
                    .target(curMapBoxLatLng) // Sets the new camera position
                    .zoom(11) // Same zoom level
                    .build();

            mapboxMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(position), 1000);
        }
    }

    private void initViews() {
        bottomSheetList = getActivity().findViewById(R.id.bottom_sheet);
        bottomSheetListBehavior = BottomSheetBehavior.from(bottomSheetList);
        bottomSheetDetails = getActivity().findViewById(R.id.bottom_sheet_details);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        fabPlus = getActivity().findViewById(R.id.fab_plus);
        fabCamera = getActivity().findViewById(R.id.fab_camera);
        fabGallery = getActivity().findViewById(R.id.fab_galery);

        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(),R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate_backward);

        transparentView = getActivity().findViewById(R.id.transparentView);

        description = getActivity().findViewById(R.id.description);
        title = getActivity().findViewById(R.id.title);
        distance = getActivity().findViewById(R.id.category);
        icon = getActivity().findViewById(R.id.icon);

        wikidataButton = getActivity().findViewById(R.id.wikidataButton);
        wikipediaButton = getActivity().findViewById(R.id.wikipediaButton);
        directionsButton = getActivity().findViewById(R.id.directionsButton);
        commonsButton = getActivity().findViewById(R.id.commonsButton);

        wikidataButtonText = getActivity().findViewById(R.id.wikidataButtonText);
        wikipediaButtonText = getActivity().findViewById(R.id.wikipediaButtonText);
        directionsButtonText = getActivity().findViewById(R.id.directionsButtonText);
        commonsButtonText = getActivity().findViewById(R.id.commonsButtonText);

    }

    private void setListeners() {
        fabPlus.setOnClickListener(view -> animateFAB(isFabOpen));

        bottomSheetDetails.setOnClickListener(view -> {
            if(bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            else{
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        bottomSheetDetailsBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                prepareViewsForSheetPosition(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0) {
                    transparentView.setAlpha(slideOffset);
                    if (slideOffset == 1) {
                        transparentView.setClickable(true);
                    } else if (slideOffset == 0){
                        transparentView.setClickable(false);
                    }
                }
            }
        });

        bottomSheetListBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        // Remove texts if it doesnt fit
        if (wikipediaButtonText.getLineCount() > 1
                || wikidataButtonText.getLineCount() > 1
                || commonsButtonText.getLineCount() > 1
                || directionsButtonText.getLineCount() > 1) {
            wikipediaButtonText.setVisibility(View.GONE);
            wikidataButtonText.setVisibility(View.GONE);
            commonsButtonText.setVisibility(View.GONE);
            directionsButtonText.setVisibility(View.GONE);
        }
    }

    private void setupMapView(Bundle savedInstanceState) {
        MapboxMapOptions options = new MapboxMapOptions()
                .styleUrl(Style.OUTDOORS)
                .camera(new CameraPosition.Builder()
                        .target(new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude()))
                        .zoom(11)
                        .build());

        // create map
        mapView = new MapView(getActivity(), options);
        mapView.onCreate(savedInstanceState);
        /*mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                NearbyMapFragment.this.mapboxMap = mapboxMap;
                NearbyMapFragment.this.mapboxMap.addMarkers(baseMarkerOptions);

                NearbyMapFragment.this.mapboxMap.setOnInfoWindowCloseListener(marker -> {
                    if (marker == selected){
                        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    }
                });

                NearbyMapFragment.this.mapboxMap.setOnMarkerClickListener(marker -> {
                    if (marker instanceof NearbyMarker) {
                        NearbyMapFragment.this.selected = marker;
                        NearbyMarker nearbyMarker = (NearbyMarker) marker;
                        Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
                        passInfoToSheet(place);
                        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                    return false;
                });
                addCurrentLocationMarker(NearbyMapFragment.this.mapboxMap);
            }
        });*/
        addNearbyMarkerstoMapBoxMap();
        //addCurrentLocationMarker(mapboxMap);
    }

    /**
     * Adds a marker for the user's current position. Adds a
     * circle which uses the accuracy * 2, to draw a circle
     * which represents the user's position with an accuracy
     * of 95%.
     *
     * Should be called only on creation of mapboxMap, there
     * is other method to update markers location with users
     * move.
     */
    private void addCurrentLocationMarker(MapboxMap mapboxMap) {
        if (currentLocationMarker != null) {
            currentLocationMarker.remove(); // Remove previous marker, we are not Hansel and Gretel
        }
        MarkerOptions currentLocationMarkerOptions = new MarkerOptions()
                .position(new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude()));
        currentLocationMarker = mapboxMap.addMarker(currentLocationMarkerOptions);


        List<LatLng> circle = createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                curLatLng.getAccuracy() * 2, 100);

        currentLocationPolygonOptions = new PolygonOptions()
                .addAll(circle)
                .strokeColor(Color.parseColor("#55000000"))
                .fillColor(Color.parseColor("#11000000"));
        mapboxMap.addPolygon(currentLocationPolygonOptions);
        //latestSignificantUpdate = curLatLng; // To remember the last point we update nearby markers
    }

    private void addNearbyMarkerstoMapBoxMap() {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

            }
        });
        mapView.getMapAsync(mapboxMap -> {
            mapboxMap.addMarkers(baseMarkerOptions);

            mapboxMap.setOnInfoWindowCloseListener(marker -> {
                if (marker == selected){
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            });

            mapboxMap.setOnMarkerClickListener(marker -> {
                if (marker instanceof NearbyMarker) {
                    this.selected = marker;
                    NearbyMarker nearbyMarker = (NearbyMarker) marker;
                    Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
                    passInfoToSheet(place);
                    bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                return false;
            });

            addCurrentLocationMarker(mapboxMap);
            NearbyMapFragment.this.mapboxMap = mapboxMap;
        });

        mapView.setStyleUrl("asset://mapstyle.json");
    }



    /**
     * Creates a series of points that create a circle on the map.
     * Takes the center latitude, center longitude of the circle,
     * the radius in meter and the number of nodes of the circle.
     *
     * @return List List of LatLng points of the circle.
     */
    private List<LatLng> createCircleArray(
            double centerLat, double centerLong, float radius, int nodes) {
        List<LatLng> circle = new ArrayList<>();
        float radiusKilometer = radius / 1000;
        double radiusLong = radiusKilometer
                / (111.320 * Math.cos(centerLat * Math.PI / 180));
        double radiusLat = radiusKilometer / 110.574;

        for (int i = 0; i < nodes; i++) {
            double theta = ((double) i / (double) nodes) * (2 * Math.PI);
            double nodeLongitude = centerLong + radiusLong * Math.cos(theta);
            double nodeLatitude = centerLat + radiusLat * Math.sin(theta);
            circle.add(new LatLng(nodeLatitude, nodeLongitude));
        }
        return circle;
    }

    public void prepareViewsForSheetPosition(int bottomSheetState) {

        switch (bottomSheetState) {
            case (BottomSheetBehavior.STATE_COLLAPSED):
                closeFabs(isFabOpen);
                if (!fabPlus.isShown()) showFAB();
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_EXPANDED):
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_HIDDEN):
                mapView.getMapAsync(MapboxMap::deselectMarkers);
                transparentView.setClickable(false);
                transparentView.setAlpha(0);
                closeFabs(isFabOpen);
                hideFAB();
                this.getView().requestFocus();
                break;
        }

    }

    private void hideFAB() {
        //get rid of anchors
        //Somehow this was the only way https://stackoverflow.com/questions/32732932/floatingactionbutton-visible-for-sometime-even-if-visibility-is-set-to-gone
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fabPlus
                .getLayoutParams();
        p.setAnchorId(View.NO_ID);
        fabPlus.setLayoutParams(p);
        fabPlus.hide();
        fabCamera.hide();
        fabGallery.hide();
    }

    private void showFAB() {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fabPlus.getLayoutParams();
        p.setAnchorId(getActivity().findViewById(R.id.bottom_sheet_details).getId());
        fabPlus.setLayoutParams(p);
        fabPlus.show();
    }

    private void passInfoToSheet(Place place) {
        this.place = place;
        wikipediaButton.setEnabled(place.hasWikipediaLink());
        wikipediaButton.setOnClickListener(view -> openWebView(place.siteLinks.getWikipediaLink()));

        wikidataButton.setEnabled(place.hasWikidataLink());
        wikidataButton.setOnClickListener(view -> openWebView(place.siteLinks.getWikidataLink()));

        directionsButton.setOnClickListener(view -> {
            //Open map app at given position
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, place.location.getGmmIntentUri());
            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            }
        });

        commonsButton.setEnabled(place.hasCommonsLink());
        commonsButton.setOnClickListener(view -> openWebView(place.siteLinks.getCommonsLink()));

        icon.setImageResource(place.getDescription().getIcon());
        description.setText(place.getDescription().getText());
        title.setText(place.name);
        distance.setText(place.distance);
    }

    private void openWebView(Uri link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, link);
        startActivity(browserIntent);
    }

    private void animateFAB(boolean isFabOpen) {

        if (isFabOpen) {

            fabPlus.startAnimation(rotate_backward);
            fabCamera.startAnimation(fab_close);
            fabGallery.startAnimation(fab_close);
            fabCamera.hide();
            fabGallery.hide();

        } else {

            fabPlus.startAnimation(rotate_forward);
            fabCamera.startAnimation(fab_open);
            fabGallery.startAnimation(fab_open);
            fabCamera.show();
            fabGallery.show();

        }

        this.isFabOpen=!isFabOpen;
    }

    private void closeFabs(boolean isFabOpen){
        if (isFabOpen) {
            fabPlus.startAnimation(rotate_backward);
            fabCamera.startAnimation(fab_close);
            fabGallery.startAnimation(fab_close);
            fabCamera.hide();
            fabGallery.hide();
            this.isFabOpen=!isFabOpen;
        }
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

    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {
        // Method is used to interpolate the marker animation.

        private LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }
}

