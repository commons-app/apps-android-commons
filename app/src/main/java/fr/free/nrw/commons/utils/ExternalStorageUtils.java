package fr.free.nrw.commons.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

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
                Log.d("deneme","Permission is granted");
                return true;
            } else {
                Log.d("deneme","Permission is revoked");
                //ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("deneme","Permission is granted");
            return true;
        }
    }

    /**
     * Requests external storage permission
     * @param context activity we are on
     */
    public static void requestExternalStoragePermission(Context context) {
        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
}
