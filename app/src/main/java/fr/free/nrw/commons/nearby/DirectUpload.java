package fr.free.nrw.commons.nearby;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
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

    DirectUpload(String title, String desc, Fragment fragment) {
        this.title = title;
        this.desc = desc;
        controller = new ContributionController(fragment);
    }

    void storeSharedPrefs(Context context) {

        Activity activity = (Activity) context;
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("Title", title);
        editor.putString("Desc", desc);
        editor.apply();

        //TODO: Shift this into title/desc screen after upload initiated
        sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        String imageTitle = sharedPref.getString("Title", "");
        String imageDesc = sharedPref.getString("Desc", "");

        Timber.d("After shared prefs, image title: " + imageTitle + " Image desc: " + imageDesc);
    }

    void initiateUpload(Context context) {
        Activity activity = (Activity) context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE)
                    != PERMISSION_GRANTED) {
                if (activity.shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(activity)
                            .setMessage(activity.getString(R.string.read_storage_permission_rationale))
                            .setPositiveButton("OK", (dialog, which) -> {
                                activity.requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 1);
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                } else {
                    activity.requestPermissions(new String[]{READ_EXTERNAL_STORAGE},
                            1);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
                else {
                    controller.startGalleryPick();
                    return true;
                }
    }
}
