package fr.free.nrw.commons.settings;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

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
        // WARNING: ORDERING NEEDS TO MATCH FOR THE LICENSE NAMES AND DISPLAY VALUES
        licensePreference.setEntries(new String[]{
                getString(R.string.license_name_cc0),
                getString(R.string.license_name_cc_by_3_0),
                getString(R.string.license_name_cc_by_4_0),
                getString(R.string.license_name_cc_by_sa_3_0),
                getString(R.string.license_name_cc_by_sa_4_0)
        });
        licensePreference.setEntryValues(new String[]{
                Prefs.Licenses.CC0,
                Prefs.Licenses.CC_BY_3,
                Prefs.Licenses.CC_BY_4,
                Prefs.Licenses.CC_BY_SA_3,
                Prefs.Licenses.CC_BY_SA_4
        });

        licensePreference.setSummary(getString(Utils.licenseNameFor(licensePreference.getValue())));
        licensePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getString(Utils.licenseNameFor((String) newValue)));
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