package fr.free.nrw.commons.LocationPicker;

import android.app.Activity;
import android.content.Intent;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import fr.free.nrw.commons.Media;

/**
 * Helper class for starting the activity
 */
public final class LocationPicker {

    /**
     * Getting camera position from the intent using constants
     *
     * @param data intent
     * @return CameraPosition
     */
    public static CameraPosition getCameraPosition(final Intent data) {
        return data.getParcelableExtra(LocationPickerConstants.MAP_CAMERA_POSITION);
    }

    public static class IntentBuilder {

        private final Intent intent;

        /**
         * Creates a new builder that creates an intent to launch the place picker activity.
         */
        public IntentBuilder() {
            intent = new Intent();
        }

        /**
         * Gets and puts location in intent
         * @param position CameraPosition
         * @return LocationPicker.IntentBuilder
         */
        public LocationPicker.IntentBuilder defaultLocation(
            final CameraPosition position) {
          intent.putExtra(LocationPickerConstants.MAP_CAMERA_POSITION, position);
          return this;
        }

        /**
         * Gets and puts activity name in intent
         * @param activity activity key
         * @return LocationPicker.IntentBuilder
         */
        public LocationPicker.IntentBuilder activityKey(
            final String activity) {
          intent.putExtra(LocationPickerConstants.ACTIVITY_KEY, activity);
          return this;
        }

        /**
         * Gets and puts media in intent
         * @param media Media
         * @return LocationPicker.IntentBuilder
         */
        public LocationPicker.IntentBuilder media(
                final Media media) {
              intent.putExtra(LocationPickerConstants.MEDIA, media);
              return this;
            }

        /**
         * Gets and sets the activity
         * @param activity Activity
         * @return Intent
         */
       public Intent build(final Activity activity) {
          intent.setClass(activity, LocationPickerActivity.class);
          return intent;
        }
    }
}
