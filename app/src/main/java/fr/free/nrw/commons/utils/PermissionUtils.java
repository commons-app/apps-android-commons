package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.BasePermissionListener;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;


public class PermissionUtils {

    /**
     * This method can be used by any activity which requires a permission which has been blocked(marked never ask again by the user)
     It open the app settings from where the user can manually give us the required permission.
     * @param activity
     */
    public static void askUserToManuallyEnablePermissionFromSettings(Activity activity) {
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

    /**
     * Checks for a particular permission and runs the runnable to perform an action when the permission is granted
     * Also, it shows a rationale if needed
     *
     * Sample usage:
     *
     * PermissionUtils.checkPermissionsAndPerformAction(activity,
     *                 Manifest.permission.WRITE_EXTERNAL_STORAGE,
     *                 () -> initiateCameraUpload(activity),
     *                 R.string.storage_permission_title,
     *                 R.string.write_storage_permission_rationale);
     *
     *
     * @param activity activity requesting permissions
     * @param permission the permission being requests
     * @param onPermissionGranted the runnable to be executed when the permission is granted
     * @param rationaleTitle rationale title to be displayed when permission was denied
     * @param rationaleMessage rationale message to be displayed when permission was denied
     */
    public static void checkPermissionsAndPerformAction(Activity activity, String permission,
        Runnable onPermissionGranted, @StringRes int rationaleTitle,
        @StringRes int rationaleMessage) {
        checkPermissionsAndPerformAction(activity, permission, onPermissionGranted, null,
            rationaleTitle, rationaleMessage);
    }

    /**
     * Checks for a particular permission and runs the corresponding runnables to perform an action when the permission is granted/denied
     * Also, it shows a rationale if needed
     *
     * Sample usage:
     *
     * PermissionUtils.checkPermissionsAndPerformAction(activity,
     *                 Manifest.permission.WRITE_EXTERNAL_STORAGE,
     *                 () -> initiateCameraUpload(activity),
     *                 () -> showMessage(),
     *                 R.string.storage_permission_title,
     *                 R.string.write_storage_permission_rationale);
     *
     *
     * @param activity activity requesting permissions
     * @param permission the permission being requests
     * @param onPermissionGranted the runnable to be executed when the permission is granted
     * @param onPermissionDenied the runnable to be executed when the permission is denied(but not permanently)
     * @param rationaleTitle rationale title to be displayed when permission was denied
     * @param rationaleMessage rationale message to be displayed when permission was denied
     */

    public static void checkPermissionsAndPerformAction(Activity activity, String permission,
        Runnable onPermissionGranted, Runnable onPermissionDenied, @StringRes int rationaleTitle,
        @StringRes int rationaleMessage) {
        Dexter.withActivity(activity)
            .withPermission(permission)
            .withListener(new BasePermissionListener() {
                @Override public void onPermissionGranted(PermissionGrantedResponse response) {
                    onPermissionGranted.run();
                }

                @Override public void onPermissionDenied(PermissionDeniedResponse response) {
                    if (response.isPermanentlyDenied()) {
                        DialogUtil.showAlertDialog(activity, activity.getString(rationaleTitle),
                            activity.getString(rationaleMessage),
                            activity.getString(R.string.navigation_item_settings), null,
                            () -> askUserToManuallyEnablePermissionFromSettings(activity), null);
                    } else {
                        if (null != onPermissionDenied) {
                            onPermissionDenied.run();
                        }
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
                    PermissionToken token) {
                    DialogUtil.showAlertDialog(activity, activity.getString(rationaleTitle),
                        activity.getString(rationaleMessage),
                        activity.getString(android.R.string.ok),
                        activity.getString(android.R.string.cancel),
                        token::continuePermissionRequest, token::cancelPermissionRequest);
                }
            })
            .check();
    }
}
