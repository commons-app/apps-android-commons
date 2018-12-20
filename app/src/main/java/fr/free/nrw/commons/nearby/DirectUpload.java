package fr.free.nrw.commons.nearby;

import android.app.Activity;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.utils.PermissionUtils;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Initiates the uploads made from a Nearby Place, in both the list and map views.
 */
class DirectUpload {

    private ContributionController controller;
    private Fragment fragment;

    DirectUpload(Fragment fragment, ContributionController controller) {
        this.fragment = fragment;
        this.controller = controller;
    }

    /**
     * Initiates the upload if user selects the Gallery FAB.
     * The permission requests will be handled by the Fragments.
     * Do not use requestCode 1 as it will conflict with NearbyFragment's requestCodes.
     */
    void initiateGalleryUpload() {
        // Only need to handle permissions for Marshmallow and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        Activity parentActivity = fragment.getActivity();
        if (parentActivity == null) {
            controller.startSingleGalleryPick();
            return;
        }

        // If we have permission, go ahead
        if (ContextCompat.checkSelfPermission(parentActivity, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            controller.startSingleGalleryPick();
            return;
        }

        // If we don't have permission, and we need to show the rationale, show the rationale
        if (fragment.shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(parentActivity)
                    .setMessage(parentActivity.getString(R.string.read_storage_permission_rationale))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        parentActivity.requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, PermissionUtils.GALLERY_PERMISSION_FROM_NEARBY_MAP);
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
            return;
        }

        // If we don't have permission, and we don't need to show rationale just request permission
        parentActivity.requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, PermissionUtils.GALLERY_PERMISSION_FROM_NEARBY_MAP);
    }

    /**
     * Initiates the upload if user selects the Camera FAB.
     * The permission requests will be handled by the Fragments.
     * Do not use requestCode 1 as it will conflict with NearbyFragment's requestCodes.
     */
    void initiateCameraUpload() {
        // Only need to handle permissions for Marshmallow and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        Activity parentActivity = fragment.getActivity();
        if (parentActivity == null) {
            controller.startCameraCapture();
            return;
        }

        // If we have permission, go ahead
        if (ContextCompat.checkSelfPermission(parentActivity, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            controller.startCameraCapture();
            return;
        }

        // If we don't have permission, and we need to show the rationale, show the rationale
        if (fragment.shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(parentActivity)
                    .setMessage(parentActivity.getString(R.string.write_storage_permission_rationale))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        parentActivity.requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, PermissionUtils.CAMERA_PERMISSION_FROM_NEARBY_MAP);
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
            return;
        }

        // If we don't have permission, and we don't need to show rationale just request permission
        parentActivity.requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, PermissionUtils.CAMERA_PERMISSION_FROM_NEARBY_MAP);
    }
}
