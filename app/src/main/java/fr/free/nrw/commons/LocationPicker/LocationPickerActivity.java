package fr.free.nrw.commons.LocationPicker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraPosition.Builder;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraIdleListener;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraMoveStartedListener;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import fr.free.nrw.commons.R;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/**
 * Helps to pick location and return the result with an intent
 */
public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback,
    OnCameraMoveStartedListener, OnCameraIdleListener, Observer<CameraPosition> {

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

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    setContentView(R.layout.activity_location_picker);

    if (savedInstanceState == null) {
      cameraPosition = getIntent().getParcelableExtra(LocationPickerConstants.MAP_CAMERA_POSITION);
    }

    final LocationPickerViewModel viewModel = new ViewModelProvider(this)
        .get(LocationPickerViewModel.class);
    viewModel.getResult().observe(this, this);

    bindViews();
    addBackButtonListener();
    addPlaceSelectedButton();
    addCredits();
    getToolbarUI();

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
    final ImageView backButton = findViewById(R.id.mapbox_place_picker_toolbar_back_button);
    backButton.setOnClickListener(view -> finish());
  }

  /**
   * Binds mapView and location picker icon
   */
  private void bindViews() {
    mapView = findViewById(R.id.map_view);
    markerImage = findViewById(R.id.location_picker_image_view_marker);
    tvAttribution = findViewById(R.id.tv_attribution);
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
      toolbar.setBackgroundColor(getResources().getColor(R.color.primaryColor));
  }

  /**
   * Takes action when map is ready to show
   * @param mapboxMap map
   */
  @Override
  public void onMapReady(final MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      adjustCameraBasedOnOptions();
      bindListeners();
      enableLocationComponent(style);
    });
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
    final FloatingActionButton placeSelectedButton = findViewById(R.id.location_chosen_button);
    placeSelectedButton.setOnClickListener(view -> placeSelected());
  }

  /**
   * Return the intent with required data
   */
  void placeSelected() {
    final Intent returningIntent = new Intent();
    returningIntent.putExtra(LocationPickerConstants.MAP_CAMERA_POSITION,
        mapboxMap.getCameraPosition());
    setResult(AppCompatActivity.RESULT_OK, returningIntent);
    finish();
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
}
