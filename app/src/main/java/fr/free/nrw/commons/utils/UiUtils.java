package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.DisplayMetrics;

import fr.free.nrw.commons.R;

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
     *
     * @param dp density independent pixels
     * @param context
     * @return px equivalent to dp value
     */
    public static float convertDpToPixel(float dp, Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Converts device specific pixels to dp.
     *
     * @param px pixels
     * @param context
     * @return dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
