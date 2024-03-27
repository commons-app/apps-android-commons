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
}
