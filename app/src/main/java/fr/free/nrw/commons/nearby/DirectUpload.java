package fr.free.nrw.commons.nearby;

import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import timber.log.Timber;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

class DirectUpload {

    private ContributionController controller;
    private Fragment fragment;

    DirectUpload(Fragment fragment, ContributionController controller) {
        this.fragment = fragment;
        this.controller = controller;
    }

    // These permission requests will be handled by the Fragments.
    // Do not use requestCode 1 as it will conflict with NearbyActivity's requestCodes
    void initiateGalleryUpload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(fragment.getActivity(), READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                if (fragment.shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(fragment.getActivity())
                            .setMessage(fragment.getActivity().getString(R.string.read_storage_permission_rationale))
                            .setPositiveButton("OK", (dialog, which) -> {
                                Timber.d("Requesting permissions for read external storage");
                                fragment.requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 4);
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                } else {
                    fragment.requestPermissions(new String[]{READ_EXTERNAL_STORAGE},
                            4);
                }
            } else {
                controller.startGalleryPick();
            }
        }
        else {
            controller.startGalleryPick();
        }
    }

    void initiateCameraUpload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(fragment.getActivity(), WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                if (fragment.shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(fragment.getActivity())
                            .setMessage(fragment.getActivity().getString(R.string.write_storage_permission_rationale))
                            .setPositiveButton("OK", (dialog, which) -> {
                                fragment.requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 5);
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                } else {
                    fragment.requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 5);
                }
            } else {
                controller.startCameraCapture();
            }
        } else {
            controller.startCameraCapture();
        }
    }
}
