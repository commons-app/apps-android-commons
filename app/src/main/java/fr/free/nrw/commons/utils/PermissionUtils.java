package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class PermissionUtils {

    //Can be used by any activity which requires a permission which has been blocked(marked never ask again by the user)
    //It open the app settings from where the user can manully give us the required permission.
    public static void askUserToManuallyEnablePermissionFromSettings(
            Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, Constants.REQUEST_CODES.OPEN_APPLICATION_DETAIL_SETTINGS);
    }
}
