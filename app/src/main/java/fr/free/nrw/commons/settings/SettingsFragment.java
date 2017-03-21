package fr.free.nrw.commons.settings;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import fr.free.nrw.commons.Prefs;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Update spinner to show selected value as summary
        ListPreference licensePreference = (ListPreference) findPreference(Prefs.DEFAULT_LICENSE);
        licensePreference.setSummary(Utils.licenseNameFor(licensePreference.getValue(),
                getActivity().getApplicationContext()));
        licensePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(Utils.licenseNameFor(((String) newValue),
                        getActivity().getApplicationContext()));
                return true;
            }
        });

        CheckBoxPreference themePreference = (CheckBoxPreference) findPreference("theme");
        themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getActivity().recreate();
                return true;
            }
        });
    }
}
