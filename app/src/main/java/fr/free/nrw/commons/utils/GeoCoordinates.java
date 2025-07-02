package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.Intent;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;

public final class GeoCoordinates {

    /**
     * Util function to handle geo coordinates. It no longer depends on google maps and any app
     * capable of handling the map intent can handle it
     *
     * @param context The context for launching intent
     * @param latLng  The latitude and longitude of the location
     */
    public static void handleGeoCoordinates(final Context context, final LatLng latLng) {
        handleGeoCoordinates(context, latLng, 16);
    }

    /**
     * Util function to handle geo coordinates with specified zoom level. It no longer depends on
     * google maps and any app capable of handling the map intent can handle it
     *
     * @param context   The context for launching intent
     * @param latLng    The latitude and longitude of the location
     * @param zoomLevel The zoom level
     */
    public static void handleGeoCoordinates(final Context context, final LatLng latLng,
        final double zoomLevel) {
        final Intent mapIntent = new Intent(Intent.ACTION_VIEW, latLng.getGmmIntentUri(zoomLevel));
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            ViewUtil.showShortToast(context, context.getString(R.string.map_application_missing));
        }
    }
}
