package fr.free.nrw.commons.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatDelegate;

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

        //Get an up button
        //settingsDelegate.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
