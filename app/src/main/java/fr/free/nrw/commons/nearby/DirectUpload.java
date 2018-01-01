package fr.free.nrw.commons.nearby;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import timber.log.Timber;

class DirectUpload {

    private String title;
    private String desc;

    DirectUpload(String title, String desc) {
        this.title = title;
        this.desc = desc;
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
}
