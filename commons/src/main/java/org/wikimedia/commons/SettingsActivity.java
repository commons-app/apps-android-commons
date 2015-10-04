package org.wikimedia.commons;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class SettingsActivity extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    CommonsApplication app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        ListPreference licensePreference = (ListPreference) findPreference(Prefs.DEFAULT_LICENSE);
        // WARNING: ORDERING NEEDS TO MATCH FOR THE LICENSE NAMES AND DISPLAY VALUES
        licensePreference.setEntries(new String[]{
                getString(R.string.license_name_cc0),
                getString(R.string.license_name_cc_by),
                getString(R.string.license_name_cc_by_sa)
        });
        licensePreference.setEntryValues(new String[]{
                Prefs.Licenses.CC0,
                Prefs.Licenses.CC_BY,
                Prefs.Licenses.CC_BY_SA
        });

        licensePreference.setSummary(getString(Utils.licenseNameFor(licensePreference.getValue())));
        licensePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getString(Utils.licenseNameFor((String)newValue)));
                return true;
            }
        });

        app = (CommonsApplication)getApplicationContext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Prefs.TRACKING_ENABLED)) {
            // We force log this, so it is logged even if EL is turned off
            EventLog.schema(CommonsApplication.EVENT_EVENTLOGGING_CHANGE)
                    .param("username", app.getCurrentAccount().name)
                    .param("state", sharedPreferences.getBoolean(key, true))
                    .log(true);
        }

    }
}
