package fr.free.nrw.commons.settings;

import android.os.Bundle;
import android.view.MenuItem;

import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;

import fr.free.nrw.commons.databinding.ActivitySettingsBinding;
import fr.free.nrw.commons.theme.BaseActivity;

/**
 * allows the user to change the settings
 */
public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;
//    private AppCompatDelegate settingsDelegate;
    /**
     * to be called when the activity starts
     * @param savedInstanceState the previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        final View view = binding.getRoot();
        setContentView(view);

        setSupportActionBar(binding.toolbarBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // Get an action bar
    /**
     * takes care of actions taken after the creation has happened
     * @param savedInstanceState the saved state
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        if (settingsDelegate == null) {
//            settingsDelegate = AppCompatDelegate.create(this, null);
//        }
//        settingsDelegate.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
