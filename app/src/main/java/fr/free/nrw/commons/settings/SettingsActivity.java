package fr.free.nrw.commons.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

public class SettingsActivity extends NavigationBaseActivity {
    private AppCompatDelegate settingsDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check prefs on every activity starts
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme",false)) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.LightAppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);
        initDrawer();
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
        //settingsDelegate.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
}