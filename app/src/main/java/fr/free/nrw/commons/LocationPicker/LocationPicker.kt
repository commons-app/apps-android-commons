package fr.free.nrw.commons.LocationPicker

import android.app.Activity
import android.content.Intent
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.Media


/**
 * Helper class for starting the activity
 */
object LocationPicker {

    /**
     * Getting camera position from the intent using constants
     *
     * @param data intent
     * @return CameraPosition
     */
    @JvmStatic
    fun getCameraPosition(data: Intent): CameraPosition? {
        return data.getParcelableExtra(LocationPickerConstants.MAP_CAMERA_POSITION)
    }

    class IntentBuilder
    /**
     * Creates a new builder that creates an intent to launch the place picker activity.
     */() {

        private val intent: Intent = Intent()

        /**
         * Gets and puts location in intent
         * @param position CameraPosition
         * @return LocationPicker.IntentBuilder
         */
        fun defaultLocation(position: CameraPosition): IntentBuilder {
            intent.putExtra(LocationPickerConstants.MAP_CAMERA_POSITION, position)
            return this
        }

        /**
         * Gets and puts activity name in intent
         * @param activity activity key
         * @return LocationPicker.IntentBuilder
         */
        fun activityKey(activity: String): IntentBuilder {
            intent.putExtra(LocationPickerConstants.ACTIVITY_KEY, activity)
            return this
        }

        /**
         * Gets and puts media in intent
         * @param media Media
         * @return LocationPicker.IntentBuilder
         */
        fun media(media: Media): IntentBuilder {
            intent.putExtra(LocationPickerConstants.MEDIA, media)
            return this
        }

        /**
         * Gets and sets the activity
         * @param activity Activity
         * @return Intent
         */
        fun build(activity: Activity): Intent {
            intent.setClass(activity, LocationPickerActivity::class.java)
            return intent
        }
    }
}