package fr.free.nrw.commons.LocationPicker;

import static com.google.android.material.snackbar.Snackbar.LENGTH_LONG;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.common.PlaceConstants;
import com.mapbox.mapboxsdk.plugins.places.common.utils.ColorUtils;
import com.mapbox.mapboxsdk.plugins.places.picker.model.PlacePickerOptions;
import com.mapbox.mapboxsdk.plugins.places.picker.ui.CurrentPlaceSelectionBottomSheet;
import com.mapbox.mapboxsdk.plugins.places.picker.viewmodel.PlacePickerViewModel;
import fr.free.nrw.commons.R;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/**
 * Helps to pick location and return the result with an intent
 */
public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback,
    MapboxMap.OnCameraMoveStartedListener, MapboxMap.OnCameraIdleListener, Observer<CarmenFeature>,
    PermissionsListener {

  private PermissionsManager permissionsManager;
  CurrentPlaceSelectionBottomSheet bottomSheet;
  CarmenFeature carmenFeature;
  private PlacePickerViewModel viewModel;
  private PlacePickerOptions options;
  private ImageView markerImage;
  private MapboxMap mapboxMap;
  private String accessToken;
  private MapView mapView;
  private FloatingActionButton userLocationButton;
  private boolean includeReverseGeocode;

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    setContentView(R.layout.mapbox_activity_place_picker);

    if (savedInstanceState == null) {
      accessToken = getIntent().getStringExtra(PlaceConstants.ACCESS_TOKEN);
      options = getIntent().getParcelableExtra(PlaceConstants.PLACE_OPTIONS);
      includeReverseGeocode = options.includeReverseGeocode();
    }

    viewModel = new ViewModelProvider(this).get(PlacePickerViewModel.class);
    viewModel.getResults().observe(this, this);

    bindViews();
    addBackButtonListener();
    addPlaceSelectedButton();
    customizeViews();

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  private void addBackButtonListener() {
    final ImageView backButton = findViewById(R.id.mapbox_place_picker_toolbar_back_button);
    backButton.setOnClickListener(view -> finish());
  }

  private void bindViews() {
    mapView = findViewById(R.id.map_view);
    bottomSheet = findViewById(R.id.mapbox_plugins_picker_bottom_sheet);
    markerImage = findViewById(R.id.mapbox_plugins_image_view_marker);
    userLocationButton = findViewById(R.id.user_location_button);
  }

  private void bindListeners() {
    mapboxMap.addOnCameraMoveStartedListener(
        this);
    mapboxMap.addOnCameraIdleListener(
        this);
  }

  private void customizeViews() {
    final ConstraintLayout toolbar = findViewById(R.id.place_picker_toolbar);
    if (options != null && options.toolbarColor() != null) {
      toolbar.setBackgroundColor(options.toolbarColor());
    } else {
      final int color = ColorUtils.getMaterialColor(this, R.attr.colorPrimary);
      toolbar.setBackgroundColor(color);
    }
  }

  @Override
  public void onMapReady(final MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      adjustCameraBasedOnOptions();
      if (includeReverseGeocode) {
        makeReverseGeocodingSearch();
      }
      bindListeners();

      if (options != null && options.includeDeviceLocationButton()) {
        enableLocationComponent(style);
      } else {
        userLocationButton.hide();
      }
    });
  }

  private void adjustCameraBasedOnOptions() {
    if (options != null) {
      if (options.startingBounds() != null) {
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(options.startingBounds(), 0));
      } else if (options.statingCameraPosition() != null) {
        mapboxMap
            .moveCamera(CameraUpdateFactory.newCameraPosition(options.statingCameraPosition()));
      }
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  private void enableLocationComponent(@NonNull final Style loadedMapStyle) {
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

      addUserLocationButton();
    } else {
      permissionsManager = new PermissionsManager(this);
      permissionsManager.requestLocationPermissions(this);
    }
  }

  @Override
  public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
      @NonNull final int[] grantResults) {
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onExplanationNeeded(final List<String> permissionsToExplain) {
    Toast.makeText(this, R.string.mapbox_plugins_place_picker_user_location_permission_explanation,
        Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(final boolean granted) {
    if (granted) {
      mapboxMap.getStyle(style -> {
        if (options != null && options.includeDeviceLocationButton()) {
          enableLocationComponent(style);
        }
      });
    }
  }

  @Override
  public void onCameraMoveStarted(final int reason) {
    Timber.v("Map camera has begun moving.");
    if (markerImage.getTranslationY() == 0) {
      markerImage.animate().translationY(-75)
          .setInterpolator(new OvershootInterpolator()).setDuration(250).start();
      if (includeReverseGeocode) {
        if (bottomSheet.isShowing()) {
          bottomSheet.dismissPlaceDetails();
        }
      }
    }
  }

  @Override
  public void onCameraIdle() {
    Timber.v("Map camera is now idling.");
    markerImage.animate().translationY(0)
        .setInterpolator(new OvershootInterpolator()).setDuration(250).start();
    if (includeReverseGeocode) {
      bottomSheet.setPlaceDetails(null);
      // Initialize with the markers current location information.
      makeReverseGeocodingSearch();
    }
  }

  @Override
  public void onChanged(@Nullable CarmenFeature carmenFeature) {
    if (carmenFeature == null) {
      carmenFeature = CarmenFeature.builder().placeName(
          String.format(Locale.US, "[%f, %f]",
              mapboxMap.getCameraPosition().target.getLatitude(),
              mapboxMap.getCameraPosition().target.getLongitude())
      ).text("No address found").properties(new JsonObject()).build();
    }
    this.carmenFeature = carmenFeature;
    bottomSheet.setPlaceDetails(carmenFeature);
  }

  private void makeReverseGeocodingSearch() {
    final LatLng latLng = mapboxMap.getCameraPosition().target;
    if (latLng != null) {
      viewModel.reverseGeocode(
          Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()),
          accessToken, options
      );
    }
  }

  private void addPlaceSelectedButton() {
    final FloatingActionButton placeSelectedButton = findViewById(R.id.place_chosen_button);
    placeSelectedButton.setOnClickListener(view -> {
      if (carmenFeature == null && includeReverseGeocode) {
        Snackbar.make(bottomSheet,
            getString(R.string.mapbox_plugins_place_picker_not_valid_selection),
            LENGTH_LONG).show();
        return;
      }
      placeSelected();
    });
  }

  /**
   * Bind the device location Floating Action Button to this activity's UI and move the
   * map camera if the button's clicked.
   */
  private void addUserLocationButton() {
    userLocationButton.show();
    userLocationButton.setOnClickListener(view -> {
      if (mapboxMap.getLocationComponent().getLastKnownLocation() != null) {
        final Location lastKnownLocation = mapboxMap.getLocationComponent().getLastKnownLocation();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
            new CameraPosition.Builder()
                .target(new LatLng(lastKnownLocation.getLatitude(),
                    lastKnownLocation.getLongitude()))
                .zoom(17.5)
                .build()
        ),1400);
      } else {
        Toast.makeText(this,
            getString(R.string.mapbox_plugins_place_picker_user_location_not_found),
            Toast.LENGTH_SHORT).show();
      }
    });
  }

  /**
   * Returning the intent with required data
   */
  void placeSelected() {
    final Intent returningIntent = new Intent();
    returningIntent.putExtra(PlaceConstants.MAP_CAMERA_POSITION, mapboxMap.getCameraPosition());
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
