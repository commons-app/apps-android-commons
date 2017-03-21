package fr.free.nrw.commons;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

public class SettingsActivity extends PreferenceActivity {
    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check prefs on every activity starts
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme",true)) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme); // default
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

        super.onCreate(savedInstanceState);
    }

    // Get an action bar
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        mDelegate.onPostCreate(savedInstanceState);
    }
}
