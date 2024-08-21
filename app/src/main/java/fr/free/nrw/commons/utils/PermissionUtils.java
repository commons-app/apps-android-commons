package fr.free.nrw.commons.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.UploadActivity;
import java.util.List;


public class PermissionUtils {

    public static String[] PERMISSIONS_STORAGE = isSDKVersionScopedStorageCompatible() ?
        isSDKVersionTiramisu() ? new String[]{
            Manifest.permission.READ_MEDIA_IMAGES} :
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
        : isSDKVersionTiramisu() ? new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES}
            : new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};

    private static boolean isSDKVersionScopedStorageCompatible() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.P;
    }

    public static boolean isSDKVersionTiramisu() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    /**
     * This method can be used by any activity which requires a permission which has been
     * blocked(marked never ask again by the user) It open the app settings from where the user can
     * manually give us the required permission.
     *
     * @param activity
     */
    private static void askUserToManuallyEnablePermissionFromSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent,
            CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS);
    }

    /**
     * Checks whether the app already has a particular permission
     *
     * @param activity
     * @param permissions permissions to be checked
     * @return
     */
    public static boolean hasPermission(Activity activity, String permissions[]) {
        boolean hasPermission = true;
        for (String permission : permissions
        ) {
            hasPermission = hasPermission &&
                ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return hasPermission;
    }

    /**
     * Checks for a particular permission and runs the runnable to perform an action when the
     * permission is granted Also, it shows a rationale if needed
     * <p>
     * rationaleTitle and rationaleMessage can be invalid @StringRes. If the value is -1 then no
     * permission rationale will be displayed and permission would be requested
     * <p>
     * Sample usage:
     * <p>
     * PermissionUtils.checkPermissionsAndPerformAction(activity,
     * Manifest.permission.WRITE_EXTERNAL_STORAGE, () -> initiateCameraUpload(activity),
     * R.string.storage_permission_title, R.string.write_storage_permission_rationale);
     * <p>
     * If you don't want the permission rationale to be shown then use:
     * <p>
     * PermissionUtils.checkPermissionsAndPerformAction(activity,
     * Manifest.permission.WRITE_EXTERNAL_STORAGE, () -> initiateCameraUpload(activity), - 1, -1);
     *
     * @param activity            activity requesting permissions
     * @param permissions         the permissions array being requests
     * @param onPermissionGranted the runnable to be executed when the permission is granted
     * @param rationaleTitle      rationale title to be displayed when permission was denied. It can
     *                            be an invalid @StringRes
     * @param rationaleMessage    rationale message to be displayed when permission was denied. It
     *                            can be an invalid @StringRes
     */
    public static void checkPermissionsAndPerformAction(Activity activity,
        Runnable onPermissionGranted, @StringRes int rationaleTitle,
        @StringRes int rationaleMessage, String... permissions) {
        checkPermissionsAndPerformAction(activity, onPermissionGranted, null,
            rationaleTitle, rationaleMessage, permissions);
    }

    /**
     * Checks for a particular permission and runs the corresponding runnables to perform an action
     * when the permission is granted/denied Also, it shows a rationale if needed
     * <p>
     * Sample usage:
     * <p>
     * PermissionUtils.checkPermissionsAndPerformAction(activity,
     * Manifest.permission.WRITE_EXTERNAL_STORAGE, () -> initiateCameraUpload(activity), () ->
     * showMessage(), R.string.storage_permission_title,
     * R.string.write_storage_permission_rationale);
     *
     * @param activity            activity requesting permissions
     * @param permissions         the permissions array being requested
     * @param onPermissionGranted the runnable to be executed when the permission is granted
     * @param onPermissionDenied  the runnable to be executed when the permission is denied(but not
     *                            permanently)
     * @param rationaleTitle      rationale title to be displayed when permission was denied
     * @param rationaleMessage    rationale message to be displayed when permission was denied
     */
    public static void checkPermissionsAndPerformAction(Activity activity,
        Runnable onPermissionGranted, Runnable onPermissionDenied, @StringRes int rationaleTitle,
        @StringRes int rationaleMessage, String... permissions) {
        Dexter.withActivity(activity)
            .withPermissions(permissions)
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    if (report.areAllPermissionsGranted()) {
                        onPermissionGranted.run();
                        return;
                    }
                    if (report.isAnyPermissionPermanentlyDenied()) {
                        // permission is denied permanently, we will show user a dialog message.
                        DialogUtil.showAlertDialog(activity, activity.getString(rationaleTitle),
                            activity.getString(rationaleMessage),
                            activity.getString(R.string.navigation_item_settings),
                            null,
                            () -> {
                                askUserToManuallyEnablePermissionFromSettings(activity);
                                if (activity instanceof UploadActivity) {
                                    ((UploadActivity) activity).setShowPermissionsDialog(true);
                                }
                            }, null, null,
                            !(activity instanceof UploadActivity));
                    } else {
                        if (null != onPermissionDenied) {
                            onPermissionDenied.run();
                        }
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                    PermissionToken token) {
                    if (rationaleTitle == -1 && rationaleMessage == -1) {
                        token.continuePermissionRequest();
                        return;
                    }
                    DialogUtil.showAlertDialog(activity, activity.getString(rationaleTitle),
                        activity.getString(rationaleMessage),
                        activity.getString(android.R.string.ok),
                        activity.getString(android.R.string.cancel),
                        () -> {
                            if (activity instanceof UploadActivity) {
                                ((UploadActivity) activity).setShowPermissionsDialog(true);
                            }
                            token.continuePermissionRequest();
                        }
                        ,
                        () -> {
                            Toast.makeText(activity.getApplicationContext(),
                                    R.string.permissions_are_required_for_functionality,
                                    Toast.LENGTH_LONG)
                                .show();
                            token.cancelPermissionRequest();
                            if (activity instanceof UploadActivity) {
                                activity.finish();
                            }
                        }
                        ,
                        null,
                        false);
                }
            })
            .onSameThread()
            .check();
    }
}
