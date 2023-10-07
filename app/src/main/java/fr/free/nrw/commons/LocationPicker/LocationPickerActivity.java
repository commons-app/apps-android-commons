package fr.free.nrw.commons.LocationPicker;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_LOCATION;
import static fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_ZOOM;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraPosition.Builder;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback;
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraIdleListener;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraMoveStartedListener;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import fr.free.nrw.commons.MapStyle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.filepicker.Constants;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationPermissionsHelper;
import fr.free.nrw.commons.location.LocationPermissionsHelper.Dialog;
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import javax.inject.Inject;
import javax.inject.Named;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/**
 * Helps to pick location and return the result with an intent
 */
public class LocationPickerActivity extends BaseActivity implements OnMapReadyCallback,
    OnCameraMoveStartedListener, OnCameraIdleListener, Observer<CameraPosition>, LocationPermissionCallback {

    /**
     * DROPPED_MARKER_LAYER_ID : id for layer
     */
    private static final String DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID";
    /**
     * cameraPosition : position of picker
     */
    private CameraPosition cameraPosition;
    /**
     * markerImage : picker image
     */
    private ImageView markerImage;
    /**
     * mapboxMap : map
     */
    private MapboxMap mapboxMap;
    /**
     * mapView : view of the map
     */
    private MapView mapView;
    /**
     * tvAttribution : credit
     */
    private AppCompatTextView tvAttribution;
    /**
     * activity : activity key
     */
    private String activity;
    /**
     * location : location
     */
    private Location location;
    /**
     * modifyLocationButton : button for start editing location
     */
    Button modifyLocationButton;
    /**
     * showInMapButton : button for showing in map
     */
    TextView showInMapButton;
    /**
     * placeSelectedButton : fab for selecting location
     */
    FloatingActionButton placeSelectedButton;
    /**
     * fabCenterOnLocation: button for center on location;
     */
    FloatingActionButton fabCenterOnLocation;
    /**
     * droppedMarkerLayer : Layer for static screen
     */
    private Layer droppedMarkerLayer;
    /**
     * shadow : imageview of shadow
     */
    private ImageView shadow;
    /**
     * largeToolbarText : textView of shadow
     */
    private TextView largeToolbarText;
    /**
     * smallToolbarText : textView of shadow
     */
    private TextView smallToolbarText;
    /**
     * applicationKvStore : for storing values
     */
    @Inject
    @Named("default_preferences")
    public
    JsonKvStore applicationKvStore;
    /**
     * isDarkTheme: for keeping a track of the device theme and modifying the map theme accordingly
     */
    @Inject
    SystemThemeUtils systemThemeUtils;
    private boolean isDarkTheme;

    @Inject
    LocationServiceManager locationManager;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);

        isDarkTheme = systemThemeUtils.isDeviceInNightMode();

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_location_picker);

        if (savedInstanceState == null) {
            cameraPosition = getIntent()
                .getParcelableExtra(LocationPickerConstants.MAP_CAMERA_POSITION);
            activity = getIntent().getStringExtra(LocationPickerConstants.ACTIVITY_KEY);
        }

        final LocationPickerViewModel viewModel = new ViewModelProvider(this)
            .get(LocationPickerViewModel.class);
        viewModel.getResult().observe(this, this);

        bindViews();
        addBackButtonListener();
        addPlaceSelectedButton();
        addCredits();
        getToolbarUI();
        addCenterOnGPSButton();

        if ("UploadActivity".equals(activity)) {
            placeSelectedButton.setVisibility(View.GONE);
            modifyLocationButton.setVisibility(View.VISIBLE);
            showInMapButton.setVisibility(View.VISIBLE);
            largeToolbarText.setText(getResources().getString(R.string.image_location));
            smallToolbarText.setText(getResources().
                getString(R.string.check_whether_location_is_correct));
            fabCenterOnLocation.setVisibility(View.GONE);
        }

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    /**
     * For showing credits
     */
    private void addCredits() {
        tvAttribution.setText(Html.fromHtml(getString(R.string.map_attribution)));
        tvAttribution.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Clicking back button destroy locationPickerActivity
     */
    private void addBackButtonListener() {
        final ImageView backButton = findViewById(R.id.maplibre_place_picker_toolbar_back_button);
        backButton.setOnClickListener(view -> finish());
    }

    /**
     * Binds mapView and location picker icon
     */
    private void bindViews() {
        mapView = findViewById(R.id.map_view);
        markerImage = findViewById(R.id.location_picker_image_view_marker);
        tvAttribution = findViewById(R.id.tv_attribution);
        modifyLocationButton = findViewById(R.id.modify_location);
        showInMapButton = findViewById(R.id.show_in_map);
        showInMapButton.setText(getResources().getString(R.string.show_in_map_app).toUpperCase());
        shadow = findViewById(R.id.location_picker_image_view_shadow);
    }

    /**
     * Binds the listeners
     */
    private void bindListeners() {
        mapboxMap.addOnCameraMoveStartedListener(
            this);
        mapboxMap.addOnCameraIdleListener(
            this);
    }

    /**
     * Gets toolbar color
     */
    private void getToolbarUI() {
        final ConstraintLayout toolbar = findViewById(R.id.location_picker_toolbar);
        largeToolbarText = findViewById(R.id.location_picker_toolbar_primary_text_view);
        smallToolbarText = findViewById(R.id.location_picker_toolbar_secondary_text_view);
        toolbar.setBackgroundColor(getResources().getColor(R.color.primaryColor));
    }

    /**
     * Takes action when map is ready to show
     * @param mapboxMap map
     */
    @Override
    public void onMapReady(final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(isDarkTheme ? MapStyle.DARK : MapStyle.STREETS, this::onStyleLoaded);
    }

    /**
     * Initializes dropped marker and layer
     * Handles camera position based on options
     * Enables location components
     *
     * @param style style
     */
    private void onStyleLoaded(final Style style) {
        if (modifyLocationButton.getVisibility() == View.VISIBLE) {
            initDroppedMarker(style);
            adjustCameraBasedOnOptions();
            enableLocationComponent(style);
            if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
                final GeoJsonSource source = style.getSourceAs("dropped-marker-source-id");
                if (source != null) {
                    source.setGeoJson(Point.fromLngLat(cameraPosition.target.getLongitude(),
                        cameraPosition.target.getLatitude()));
                }
                droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID);
                if (droppedMarkerLayer != null) {
                    droppedMarkerLayer.setProperties(visibility(VISIBLE));
                    markerImage.setVisibility(View.GONE);
                    shadow.setVisibility(View.GONE);
                }
            }
        } else {
            adjustCameraBasedOnOptions();
            enableLocationComponent(style);
            bindListeners();
        }

        modifyLocationButton.setOnClickListener(v -> onClickModifyLocation());
        showInMapButton.setOnClickListener(v -> showInMap());
    }

    /**
     * Handles onclick event of modifyLocationButton
     */
    private void onClickModifyLocation() {
        placeSelectedButton.setVisibility(View.VISIBLE);
        modifyLocationButton.setVisibility(View.GONE);
        showInMapButton.setVisibility(View.GONE);
        droppedMarkerLayer.setProperties(visibility(NONE));
        markerImage.setVisibility(View.VISIBLE);
        shadow.setVisibility(View.VISIBLE);
        largeToolbarText.setText(getResources().getString(R.string.choose_a_location));
        smallToolbarText.setText(getResources().getString(R.string.pan_and_zoom_to_adjust));
        bindListeners();
        fabCenterOnLocation.setVisibility(View.VISIBLE);
    }

    /**
     * Show the location in map app
     */
    public void showInMap() {
        Utils.handleGeoCoordinates(this,
            new fr.free.nrw.commons.location.LatLng(cameraPosition.target.getLatitude(),
                cameraPosition.target.getLongitude(), 0.0f));
    }

    /**
     * Initialize Dropped Marker and layer without showing
     * @param loadedMapStyle style
     */
    private void initDroppedMarker(@NonNull final Style loadedMapStyle) {
        // Add the marker image to map
        loadedMapStyle.addImage("dropped-icon-image", BitmapFactory.decodeResource(
            getResources(), R.drawable.map_default_map_marker));
        loadedMapStyle.addSource(new GeoJsonSource("dropped-marker-source-id"));
        loadedMapStyle.addLayer(new SymbolLayer(DROPPED_MARKER_LAYER_ID,
            "dropped-marker-source-id").withProperties(
            iconImage("dropped-icon-image"),
            visibility(NONE),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        ));
    }

    /**
     * move the location to the current media coordinates
     */
    private void adjustCameraBasedOnOptions() {
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Enables location components
     * @param loadedMapStyle Style
     */
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull final Style loadedMapStyle) {
        final UiSettings uiSettings = mapboxMap.getUiSettings();
        uiSettings.setAttributionEnabled(false);

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            final LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.NONE);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.NORMAL);

            // Get the component's location engine to receive user's last location
            locationComponent.getLocationEngine().getLastLocation(
                new LocationEngineCallback<LocationEngineResult>() {
                    @Override
                    public void onSuccess(LocationEngineResult result) {
                        location = result.getLastLocation();
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                    }
                });


        } else {
            requestLocationPermissions();
        }
    }

    /**
     * Acts on camera moving
     * @param reason int
     */
    @Override
    public void onCameraMoveStarted(final int reason) {
        Timber.v("Map camera has begun moving.");
        if (markerImage.getTranslationY() == 0) {
            markerImage.animate().translationY(-75)
                .setInterpolator(new OvershootInterpolator()).setDuration(250).start();
        }
    }

    /**
     * Acts on camera idle
     */
    @Override
    public void onCameraIdle() {
        Timber.v("Map camera is now idling.");
        markerImage.animate().translationY(0)
            .setInterpolator(new OvershootInterpolator()).setDuration(250).start();
    }

    /**
     * Takes action on camera position
     * @param position position of picker
     */
    @Override
    public void onChanged(@Nullable CameraPosition position) {
        if (position == null) {
            position = new Builder()
                .target(new LatLng(mapboxMap.getCameraPosition().target.getLatitude(),
                    mapboxMap.getCameraPosition().target.getLongitude()))
                .zoom(16).build();
        }
        cameraPosition = position;
    }

    /**
     * Select the preferable location
     */
    private void addPlaceSelectedButton() {
        placeSelectedButton = findViewById(R.id.location_chosen_button);
        placeSelectedButton.setOnClickListener(view -> placeSelected());
    }

    /**
     * Return the intent with required data
     */
    void placeSelected() {
        if (activity.equals("NoLocationUploadActivity")) {
            applicationKvStore.putString(LAST_LOCATION,
                mapboxMap.getCameraPosition().target.getLatitude()
                    + ","
                    + mapboxMap.getCameraPosition().target.getLongitude());
            applicationKvStore.putString(LAST_ZOOM, mapboxMap.getCameraPosition().zoom + "");
        }
        final Intent returningIntent = new Intent();
        returningIntent.putExtra(LocationPickerConstants.MAP_CAMERA_POSITION,
            mapboxMap.getCameraPosition());
        setResult(AppCompatActivity.RESULT_OK, returningIntent);
        finish();
    }
    /**
     * Center the camera on the last saved location
     */
    private void addCenterOnGPSButton(){
        fabCenterOnLocation = findViewById(R.id.center_on_gps);
        fabCenterOnLocation.setOnClickListener(view -> requestLocationPermissions());
    }

    /**
     * Center the map at user's current location
     */
    private void requestLocationPermissions() {
        LocationPermissionsHelper.Dialog locationAccessDialog = new Dialog(
            R.string.location_permission_title,
            R.string.upload_map_location_access
        );

        LocationPermissionsHelper.Dialog locationOffDialog = new Dialog(
            R.string.ask_to_turn_location_on,
            R.string.upload_map_location_access
        );
        LocationPermissionsHelper locationPermissionsHelper = new LocationPermissionsHelper(
            this, locationManager, this);
        locationPermissionsHelper.handleLocationPermissions(locationAccessDialog, locationOffDialog);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
        @NonNull final int[] grantResults) {
        if (requestCode == Constants.RequestCodes.LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted();
        } else {
            onLocationPermissionDenied("");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(final @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onLocationPermissionDenied(String toastMessage) {
        //do nothing
    }

    @Override
    public void onLocationPermissionGranted() {
        if (mapboxMap.getStyle() != null) {
            enableLocationComponent(mapboxMap.getStyle());
        }
        fr.free.nrw.commons.location.LatLng currLocation = locationManager.getLastLocation();
        if (currLocation != null) {
            final CameraPosition position;
            position = new CameraPosition.Builder()
                .target(new com.mapbox.mapboxsdk.geometry.LatLng(currLocation.getLatitude(),
                    currLocation.getLongitude(), 0)) // Sets the new camera position
                .zoom(mapboxMap.getCameraPosition().zoom) // Same zoom level
                .build();

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
        }
    }
}
