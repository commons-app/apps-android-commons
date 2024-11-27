package fr.free.nrw.commons.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import fr.free.nrw.commons.R
import fr.free.nrw.commons.upload.UploadActivity


object PermissionUtils {

    @JvmStatic
    val PERMISSIONS_STORAGE: Array<String> = getPermissionsStorage()

    @JvmStatic
    private fun getPermissionsStorage(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * This method can be used by any activity which requires a permission which has been
     * blocked(marked never ask again by the user) It open the app settings from where the user can
     * manually give us the required permission.
     *
     * @param activity The Activity which requires a permission which has been blocked
     */
    @JvmStatic
    private fun askUserToManuallyEnablePermissionFromSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    /**
     * Checks whether the app already has a particular permission
     *
     * @param activity The Activity context to check permissions against
     * @param permissions An array of permission strings to check
     * @return `true if the app has all the specified permissions, `false` otherwise
     */
    @JvmStatic
    fun hasPermission(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if the app has partial access permissions.
     */
    @JvmStatic
    fun hasPartialAccess(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        activity, Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_DENIED
        } else false
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
    @JvmStatic
    fun checkPermissionsAndPerformAction(
        activity: Activity,
        onPermissionGranted: Runnable,
        rationaleTitle: Int,
        rationaleMessage: Int,
        vararg permissions: String
    ) {
        if (hasPartialAccess(activity)) {
            Thread(onPermissionGranted).start()
            return
        }
        checkPermissionsAndPerformAction(
            activity, onPermissionGranted, null, rationaleTitle, rationaleMessage, *permissions
        )
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
    @JvmStatic
    fun checkPermissionsAndPerformAction(
        activity: Activity,
        onPermissionGranted: Runnable,
        onPermissionDenied: Runnable? = null,
        rationaleTitle: Int,
        rationaleMessage: Int,
        vararg permissions: String
    ) {
        Dexter.withActivity(activity)
            .withPermissions(*permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    when {
                        report.areAllPermissionsGranted() || hasPartialAccess(activity) ->
                            Thread(onPermissionGranted).start()
                        report.isAnyPermissionPermanentlyDenied -> {
                            DialogUtil.showAlertDialog(
                                activity,
                                activity.getString(rationaleTitle),
                                activity.getString(rationaleMessage),
                                activity.getString(R.string.navigation_item_settings),
                                null,
                                {
                                    askUserToManuallyEnablePermissionFromSettings(activity)
                                    if (activity is UploadActivity) {
                                        activity.isShowPermissionsDialog = true
                                    }
                                },
                                null, null, activity !is UploadActivity
                            )
                        }
                        else -> Thread(onPermissionDenied).start()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>, token: PermissionToken
                ) {
                    if (rationaleTitle == -1 && rationaleMessage == -1) {
                        token.continuePermissionRequest()
                        return
                    }
                    DialogUtil.showAlertDialog(
                        activity,
                        activity.getString(rationaleTitle),
                        activity.getString(rationaleMessage),
                        activity.getString(android.R.string.ok),
                        activity.getString(android.R.string.cancel),
                        {
                            if (activity is UploadActivity) {
                                activity.setShowPermissionsDialog(true)
                            }
                            token.continuePermissionRequest()
                        },
                        {
                            Toast.makeText(
                                activity.applicationContext,
                                R.string.permissions_are_required_for_functionality,
                                Toast.LENGTH_LONG
                            ).show()
                            token.cancelPermissionRequest()
                            if (activity is UploadActivity) {
                                activity.finish()
                            }
                        },
                        null, false
                    )
                }
            }).onSameThread().check()
    }
}
