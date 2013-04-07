package org.wikimedia.commons;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class SettingsActivity extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    CommonsApplication app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
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
