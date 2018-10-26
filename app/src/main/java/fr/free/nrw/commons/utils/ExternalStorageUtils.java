package fr.free.nrw.commons.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import timber.log.Timber;

/**
 * Created by root on 23.07.2018.
 */

public class ExternalStorageUtils {

    /**
     * Checks if external storage permission is granted
     * @param context activity we are on
     * @return true if permission is granted, false if not
     */
    public static boolean isStoragePermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Timber.d("External storage permission granted, API >= 23");
                return true;
            } else {
                Timber.d("External storage permission not granted, API >= 23");
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Timber.d("External storage permission granted before, API < 23");
            return true;
        }
    }

    /**
     * Requests external storage permission
     * @param context activity we are on
     */
    public static void requestExternalStoragePermission(Context context) {
        Timber.d("External storage permission requested");
        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
}
