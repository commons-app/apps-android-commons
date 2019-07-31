package fr.free.nrw.commons.utils;

import android.util.Log;
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
                Log.d("deneme7","heig:"+layoutParams.height+"weig:"+view.getWidth() * rate);
                layoutParams.height = (int) (view.getWidth() * rate);
                view.setLayoutParams(layoutParams);
            }
        });
    }

    /**
     * Can be used for keeping aspect radios suggested by material guidelines. See:
     * https://material.io/design/layout/spacing-methods.html#containers-aspect-ratios
     * In some cases we don't know exact height, for such cases this method measures
     * height and sets width by multiplying the width with height.
     * @param rate Aspect ratios, ie 1 for 1:1. (height * rate = width)
     * @param view view to change width
     */
    public static void setLayoutWidthAllignedToHeight(double rate, View view) {
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.width = (int) (view.getHeight() * rate);
                view.setLayoutParams(layoutParams);
            }
        });
    }
}
