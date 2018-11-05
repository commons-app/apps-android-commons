package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;

import fr.free.nrw.commons.CommonsApplication;

public class PermissionUtils {

    public static final int CAMERA_PERMISSION_FROM_CONTRIBUTION_LIST = 100;
    public static final int GALLERY_PERMISSION_FROM_CONTRIBUTION_LIST = 101;
    public static final int CAMERA_PERMISSION_FROM_NEARBY_MAP = 102;
    public static final int GALLERY_PERMISSION_FROM_NEARBY_MAP = 103;

    /**
     * This method can be used by any activity which requires a permission which has been blocked(marked never ask again by the user)
     It open the app settings from where the user can manually give us the required permission.
     * @param activity
     */
    public static void askUserToManuallyEnablePermissionFromSettings(
            Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent,CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS);
    }

    /**
     * Checks whether the app already has a particular permission
     *
     * @param activity
     * @param permission permission to be checked
     * @return
     */
    public static boolean hasPermission(Activity activity, String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;

    }
}
