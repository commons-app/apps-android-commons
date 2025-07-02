package fr.free.nrw.commons.utils;

import android.graphics.Bitmap;
import android.view.View;

public final class ScreenCaptureUtils {

    /**
     * To take screenshot of the screen and return it in Bitmap format
     *
     * @param view
     * @return
     */
    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap drawingCache = screenView.getDrawingCache();
        if (drawingCache != null) {
            Bitmap bitmap = Bitmap.createBitmap(drawingCache);
            screenView.setDrawingCacheEnabled(false);
            return bitmap;
        }
        return null;
    }
}
