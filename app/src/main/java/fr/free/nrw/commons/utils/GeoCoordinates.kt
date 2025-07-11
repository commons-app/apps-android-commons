package fr.free.nrw.commons.utils

import android.content.Context
import android.content.Intent
import fr.free.nrw.commons.R
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.utils.ViewUtil.showShortToast

/**
 * Util function to handle geo coordinates with specified zoom level. It no longer depends on
 * google maps and any app capable of handling the map intent can handle it
 *
 * @param context   The context for launching intent
 * @param latLng    The latitude and longitude of the location
 * @param zoomLevel The zoom level
 */
fun handleGeoCoordinates(
    context: Context, latLng: LatLng,
    zoomLevel: Double = 16.0
) {
    val mapIntent = Intent(Intent.ACTION_VIEW, latLng.getGmmIntentUri(zoomLevel))
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        showShortToast(context, context.getString(R.string.map_application_missing))
    }
}
