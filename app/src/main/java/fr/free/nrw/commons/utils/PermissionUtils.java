package fr.free.nrw.commons.utils;

import android.Manifest;
import android.Manifest.permission;
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
    public static String[] PERMISSIONS_STORAGE = getPermissionsStorage();

    static String[] getPermissionsStorage() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return new String[]{ Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.READ_MEDIA_IMAGES };
        }
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            return new String[]{ Manifest.permission.READ_MEDIA_IMAGES };
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE };
        }
        return new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    }

    /**
     * This method can be used by any activity which requires a permission which has been
     * blocked(marked never ask again by the user) It open the app settings from where the user can
     * manually give us the required permission.
     *
     * @param activity The Activity which requires a permission which has been blocked
     */
    private static void askUserToManuallyEnablePermissionFromSettings(final Activity activity) {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        final Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent,
            CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS);
    }

    /**
     * Checks whether the app already has a particular permission
     *
     * @param activity The Activity context to check permissions against
     * @param permissions An array of permission strings to check
     * @return `true if the app has all the specified permissions, `false` otherwise
     */
    public static boolean hasPermission(final Activity activity, final String[] permissions) {
        boolean hasPermission = true;
        for(final String permission : permissions) {
            hasPermission = hasPermission &&
                ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return hasPermission;
    }

    public static boolean hasPartialAccess(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return ContextCompat.checkSelfPermission(activity,
                permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
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
    public static void checkPermissionsAndPerformAction(
        final Activity activity,
        final Runnable onPermissionGranted,
        final @StringRes int rationaleTitle,
        final @StringRes int rationaleMessage,
        final String... permissions
    ) {
        if (hasPartialAccess(activity)) {
            onPermissionGranted.run();
            return;
        }
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
    public static void checkPermissionsAndPerformAction(
        final Activity activity,
        final Runnable onPermissionGranted,
        final Runnable onPermissionDenied,
        final @StringRes int rationaleTitle,
        final @StringRes int rationaleMessage,
        final String... permissions
    ) {
        Dexter.withActivity(activity)
            .withPermissions(permissions)
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(final MultiplePermissionsReport report) {
                    if (report.areAllPermissionsGranted() || hasPartialAccess(activity)) {
                        onPermissionGranted.run();
                        return;
                    }
                    if (report.isAnyPermissionPermanentlyDenied()) {
                        // permission is denied permanently, we will show user a dialog message.
                        DialogUtil.showAlertDialog(
                            activity, activity.getString(rationaleTitle),
                            activity.getString(rationaleMessage),
                            activity.getString(R.string.navigation_item_settings),
                            null, () -> {
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
                public void onPermissionRationaleShouldBeShown(
                    final List<PermissionRequest> permissions,
                    final PermissionToken token
                ) {
                    if (rationaleTitle == -1 && rationaleMessage == -1) {
                        token.continuePermissionRequest();
                        return;
                    }
                    DialogUtil.showAlertDialog(
                        activity, activity.getString(rationaleTitle),
                        activity.getString(rationaleMessage),
                        activity.getString(android.R.string.ok),
                        activity.getString(android.R.string.cancel),
                        () -> {
                            if (activity instanceof UploadActivity) {
                                ((UploadActivity) activity).setShowPermissionsDialog(true);
                            }
                            token.continuePermissionRequest();
                        },
                        () -> {
                            Toast.makeText(activity.getApplicationContext(),
                                R.string.permissions_are_required_for_functionality,
                                Toast.LENGTH_LONG
                            ).show();
                            token.cancelPermissionRequest();
                            if (activity instanceof UploadActivity) {
                                activity.finish();
                            }
                        }, null, false
                    );
                }
            }).onSameThread().check();
    }
}
