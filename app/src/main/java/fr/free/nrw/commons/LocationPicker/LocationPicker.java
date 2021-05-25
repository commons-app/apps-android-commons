package fr.free.nrw.commons.LocationPicker;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.plugins.places.common.PlaceConstants;
import com.mapbox.mapboxsdk.plugins.places.picker.model.PlacePickerOptions;

/**
 * Helper class for starting the activity
 */
public final class LocationPicker {

  public static CameraPosition getCameraPosition(final Intent data) {
    return data.getParcelableExtra(PlaceConstants.MAP_CAMERA_POSITION);
  }

  public static class IntentBuilder {

    private final Intent intent;

    /**
     * Creates a new builder that creates an intent to launch the place picker activity.
     */
    public IntentBuilder() {
      intent = new Intent();
    }

    public LocationPicker.IntentBuilder accessToken(@NonNull final String accessToken) {
      intent.putExtra(PlaceConstants.ACCESS_TOKEN, accessToken);
      return this;
    }

    public LocationPicker.IntentBuilder placeOptions(
        final PlacePickerOptions placeOptions) {
      intent.putExtra(PlaceConstants.PLACE_OPTIONS, placeOptions);
      return this;
    }

    public Intent build(final Activity activity) {
      intent.setClass(activity, LocationPickerActivity.class);
      return intent;
    }
  }
}
