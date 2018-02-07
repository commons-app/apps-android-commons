package fr.free.nrw.commons.nearby;

import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;

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

    void initiateCameraUpload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(fragment.getActivity(), WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                if (fragment.getActivity().shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(fragment.getActivity())
                            .setMessage(fragment.getActivity().getString(R.string.write_storage_permission_rationale))
                            .setPositiveButton("OK", (dialog, which) -> {
                                fragment.getActivity().requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 3);
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                } else {
                    fragment.getActivity().requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 3);
                }
            } else {
                controller.startCameraCapture();
            }
        } else {
            controller.startCameraCapture();
        }
    }

    void initiateGalleryUpload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(fragment.getActivity(), READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                if (fragment.getActivity().shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(fragment.getActivity())
                            .setMessage(fragment.getActivity().getString(R.string.read_storage_permission_rationale))
                            .setPositiveButton("OK", (dialog, which) -> {
                                fragment.getActivity().requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 1);
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                } else {
                    fragment.getActivity().requestPermissions(new String[]{READ_EXTERNAL_STORAGE},
                            1);
                }
            } else {
                controller.startGalleryPick();
            }
        }
        else {
            controller.startGalleryPick();
        }
    }
}
