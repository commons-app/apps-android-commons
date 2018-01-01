package fr.free.nrw.commons.nearby;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import timber.log.Timber;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

class DirectUpload {

    private String title;
    private String desc;
    private ContributionController controller;
    private Fragment fragment;

    DirectUpload(String title, String desc, Fragment fragment) {
        this.title = title;
        this.desc = desc;
        this.fragment = fragment;
        controller = new ContributionController(fragment);
    }

    void storeSharedPrefs() {
        SharedPreferences sharedPref = fragment.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("Title", title);
        editor.putString("Desc", desc);
        editor.apply();

        //TODO: Shift this into title/desc screen after upload initiated
        sharedPref = fragment.getActivity().getPreferences(Context.MODE_PRIVATE);
        String imageTitle = sharedPref.getString("Title", "");
        String imageDesc = sharedPref.getString("Desc", "");

        Timber.d("After shared prefs, image title: " + imageTitle + " Image desc: " + imageDesc);
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


//TODO: Handle onRequestPermissionsResult
}
