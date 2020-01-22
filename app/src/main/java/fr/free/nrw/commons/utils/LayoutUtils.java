package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public class LayoutUtils {

    /**
     * Can be used for keeping aspect radios suggested by material guidelines. See:
     * https://material.io/design/layout/spacing-methods.html#containers-aspect-ratios
     * In some cases we don't know exact width, for such cases this method measures
     * width and sets height by multiplying the width with height.
     * @param rate Aspect ratios, ie 1 for 1:1. (width * rate = height)
     * @param view view to change height
     */
    public static void setLayoutHeightAllignedToWidth(double rate, View view) {
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.height = (int) (view.getWidth() * rate);
                view.setLayoutParams(layoutParams);
            }
        });
    }

    public static double getScreenWidth(Context context, double rate) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels * rate;
    }
}