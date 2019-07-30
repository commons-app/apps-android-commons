package fr.free.nrw.commons.utils;

import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;

public class LayoutUtils {
    public static ViewGroup.LayoutParams getLayoutParamsHeightByWindowRate(WindowManager windowManager,
                                                                               double rate,
                                                                               ViewGroup.LayoutParams layoutParams) {
        Display display = windowManager.getDefaultDisplay();
        int height = display.getHeight();
        layoutParams.height = (int) Math.round(height*rate);
        return layoutParams;
    }

    public static ViewGroup.LayoutParams getLayoutParamsWidthByWindowRate(WindowManager windowManager,
                                                                           double rate,
                                                                           ViewGroup.LayoutParams layoutParams) {
        Display display = windowManager.getDefaultDisplay();
        int width = display.getWidth();
        layoutParams.width = (int) Math.round(width*rate);
        return layoutParams;
    }
}
