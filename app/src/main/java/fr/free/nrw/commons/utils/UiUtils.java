package fr.free.nrw.commons.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.util.ArrayList;
import java.util.List;

public class UiUtils {

    /**
     * Draws a vectorial image onto a bitmap.
     * @param vectorDrawable vectorial image
     * @return bitmap representation of the vectorial image
     */
    public static Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    /**
     * Converts dp unit to equivalent pixels.
     * @param dp density independent pixels
     * @param context Context to access display metrics
     * @return px equivalent to dp value
     */
    public static float convertDpToPixel(float dp, Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Creates a series of points that create a circle on the map.
     * Takes the center latitude, center longitude of the circle,
     * the radius in meter and the number of nodes of the circle.
     *
     * @return List List of LatLng points of the circle.
     */
    public static List<com.mapbox.mapboxsdk.geometry.LatLng> createCircleArray(
            double centerLat, double centerLong, float radius, int nodes) {
        List<com.mapbox.mapboxsdk.geometry.LatLng> circle = new ArrayList<>();
        float radiusKilometer = radius / 1000;
        double radiusLong = radiusKilometer
                / (111.320 * Math.cos(centerLat * Math.PI / 180));
        double radiusLat = radiusKilometer / 110.574;

        for (int i = 0; i < nodes; i++) {
            double theta = ((double) i / (double) nodes) * (2 * Math.PI);
            double nodeLongitude = centerLong + radiusLong * Math.cos(theta);
            double nodeLatitude = centerLat + radiusLat * Math.sin(theta);
            circle.add(new com.mapbox.mapboxsdk.geometry.LatLng(nodeLatitude, nodeLongitude));
        }
        return circle;
    }
}
