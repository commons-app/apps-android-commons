package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.widget.Toast
import fr.free.nrw.commons.R

/**
 * Provides tooltip actions for the Nearby place controls.
 *
 * @param activity activity used to display the tooltips
 */
open class PlaceLongPressActions(
    protected val activity: Activity,
) {
    /**
     * Shows the camera tooltip.
     *
     * @return a long-press action that consumes the event
     */
    fun onCameraLongPressed(): () -> Boolean =
        {
            Toast.makeText(activity, R.string.menu_from_camera, Toast.LENGTH_SHORT).show()
            true
        }

    /**
     * Shows the gallery tooltip.
     *
     * @return a long-press action that consumes the event
     */
    fun onGalleryLongPressed(): () -> Boolean =
        {
            Toast.makeText(activity, R.string.menu_from_gallery, Toast.LENGTH_SHORT).show()
            true
        }

    /**
     * Shows the bookmark tooltip.
     *
     * @return a long-press action that consumes the event
     */
    fun onBookmarkLongPressed(): () -> Boolean =
        {
            Toast.makeText(activity, R.string.menu_bookmark, Toast.LENGTH_SHORT).show()
            true
        }

    /**
     * Shows the directions tooltip.
     *
     * @return a long-press action that consumes the event
     */
    fun onDirectionsLongPressed(): () -> Boolean =
        {
            Toast.makeText(activity, R.string.nearby_directions, Toast.LENGTH_SHORT).show()
            true
        }

    /**
     * Shows the overflow-menu tooltip.
     *
     * @return a long-press action that consumes the event
     */
    fun onOverflowLongPressed(): () -> Boolean =
        {
            Toast.makeText(activity, R.string.more, Toast.LENGTH_SHORT).show()
            true
        }
}
