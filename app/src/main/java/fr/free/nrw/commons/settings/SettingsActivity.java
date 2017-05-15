package fr.free.nrw.commons.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;

import fr.free.nrw.commons.R;

public class SettingsActivity extends PreferenceActivity {
    private AppCompatDelegate settingsDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check prefs on every activity starts
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme",true)) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();

        super.onCreate(savedInstanceState);
    }

    // Get an action bar
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (settingsDelegate == null) {
            settingsDelegate = AppCompatDelegate.create(this, null);
        }
        settingsDelegate.onPostCreate(savedInstanceState);

        //Get an up button
        settingsDelegate.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    //Handle action-bar clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void startYourself(Context context) {
        Intent settingsIntent = new Intent(context, SettingsActivity.class);
        context.startActivity(settingsIntent);
    }
}