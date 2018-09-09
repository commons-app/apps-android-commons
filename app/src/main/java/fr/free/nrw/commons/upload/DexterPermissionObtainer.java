package fr.free.nrw.commons.upload;

import android.Manifest;
import android.app.Activity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ExternalStorageUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import timber.log.Timber;

public class DexterPermissionObtainer {
    private final String TAG = "#MultipleShareActivity#";
    private final String requestedPermission;
    private android.app.AlertDialog storagePermissionInfoDialog;
    private DexterBuilder dexterStoragePermissionBuilder;

    private PermissionDeniedResponse permissionDeniedResponse;

    private boolean receivedSharedItems;
    private boolean storagePromptInProgress;
    private Runnable onPermissionObtained;

    private final String rationaleTitle;
    private final String rationaleText;

    private Activity activity;

    /**
     * @param activity The activity that is requesting the permission
     * @param requestedPermission The permission being requested in the form of Manifest.permission.*
     * @param rationaleTitle The title of the rationale dialog
     * @param rationaleText The text inside the rationale dialog
     * @param onPermissionObtained The function to be called when the permission is obtained.
     */
    public DexterPermissionObtainer(Activity activity, String requestedPermission, String rationaleTitle, String rationaleText, Runnable onPermissionObtained){
        this.activity=activity;
        this.rationaleTitle = rationaleTitle;
        this.rationaleText = rationaleText;
        this.requestedPermission=requestedPermission;
        this.onPermissionObtained=onPermissionObtained;
        initPermissionsRationaleDialog();
    }


    /**
     * Checks if storage permissions are obtained, prompts the users to grant storage permissions if necessary.
     * When storage permission is present, onPermissionObtained is called.
     */
    public void confirmStoragePermissions() {
        if (ExternalStorageUtils.isStoragePermissionGranted(activity)) {
            Timber.i("Storage permissions already granted.");
            onPermissionObtained.run();
        } else if (!storagePromptInProgress) {
            //If permission is not there, ask for it
            storagePromptInProgress = true;
            askDexterToHandleExternalStoragePermission();
        }
        //return storagePermissionReady;return null;
    }


    /**
     * To be called when the user returns to the original activity after manually enabling storage permissions.
     */
    public void onManualPermissionReturned(){
        //OnActivity result, no matter what the result is, our function can handle that.
        askDexterToHandleExternalStoragePermission();
    }

    /**
     * This method initialised the Dexter's permission builder (if not already initialised). Also makes sure that the builder is initialised
     * only once, otherwise we would'nt know on which instance of it, the user is working on. And after the builder is initialised, it checks
     * for the required permission and then handles the permission status, thanks to Dexter's appropriate callbacks.
     */
    private void askDexterToHandleExternalStoragePermission() {
        Timber.d(TAG, "External storage permission is being requested");
        if (null == dexterStoragePermissionBuilder) {
            dexterStoragePermissionBuilder = Dexter.withActivity(activity)
                    .withPermission(requestedPermission)
                    .withListener(new BasePermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            Timber.d(TAG, "User has granted us the permission for writing the external storage");
                            //If permission is granted, well and good
                            storagePromptInProgress = false;
                            onPermissionObtained.run();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Timber.d(TAG, "User has granted us the permission for writing the external storage");
                            //If permission is not granted in whatsoever scenario, we show him a dialog stating why we need the permission
                            permissionDeniedResponse = response;
                            if (null != storagePermissionInfoDialog && !storagePermissionInfoDialog
                                    .isShowing()) {
                                storagePermissionInfoDialog.show();
                            }
                        }
                    });
        }
        dexterStoragePermissionBuilder.check();
    }

    /**
     * We have agreed to show a dialog showing why we need a particular permission.
     * This method is used to initialise the dialog which is going to show the permission's rationale.
     * The dialog is initialised along with a callback for positive and negative user actions.
     */
    private void initPermissionsRationaleDialog() {
        if (storagePermissionInfoDialog == null) {
            storagePermissionInfoDialog = DialogUtil
                    .getAlertDialogWithPositiveAndNegativeCallbacks(
                            activity,
                            rationaleTitle, rationaleText,
                            R.drawable.ic_launcher, new DialogUtil.Callback() {
                                @Override
                                public void onPositiveButtonClicked() {
                                    //If the user is willing to give us the permission
                                    //But had somehow previously choose never ask again, we take him to app settings to manually enable permission
                                    if (null == permissionDeniedResponse) {
                                        //Dexter returned null, lets see if this ever happens
                                        Timber.w("Dexter returned null as permissionDeniedResponse");
                                    } else if (permissionDeniedResponse.isPermanentlyDenied()) {
                                        PermissionUtils.askUserToManuallyEnablePermissionFromSettings(activity);
                                        Timber.i("Permission permanently denied.");
                                    } else {
                                        //or if we still have chance to show runtime permission dialog, we show him that.
                                        askDexterToHandleExternalStoragePermission();
                                        Timber.d("Asking via Dexter for permission.");
                                    }
                                }

                                @Override
                                public void onNegativeButtonClicked() {
                                    //This was the behaviour as of now, I was planning to maybe snack him with some message
                                    //and then call finish after some time, or may be it could be associated with some action
                                    // on the snack. If the user does not want us to give the permission, even after showing
                                    // rationale dialog, lets not trouble him any more.
                                    activity.finish();
                                }
                            });
        }
    }
}
