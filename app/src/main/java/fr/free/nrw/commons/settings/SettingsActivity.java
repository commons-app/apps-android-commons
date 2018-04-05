package fr.free.nrw.commons.settings;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

/**
 * allows the user to change the settings
 */
public class SettingsActivity extends NavigationBaseActivity {
    private AppCompatDelegate settingsDelegate;

    /**
     * to be called when the activity starts
     * @param savedInstanceState the previously saved state
     */
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
    /**
     * takes care of actions taken after the creation has happened
     * @param savedInstanceState the saved state
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (settingsDelegate == null) {
            settingsDelegate = AppCompatDelegate.create(this, null);
        }
        settingsDelegate.onPostCreate(savedInstanceState);
    }
    
    /**
     * Handle action-bar clicks
     * @param item the selected item
     * @return true on success, false on failure
     */
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