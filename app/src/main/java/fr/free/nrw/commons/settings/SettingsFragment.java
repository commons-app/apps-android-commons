package fr.free.nrw.commons.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

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
        licensePreference
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
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

        final EditTextPreference uploadLimit = (EditTextPreference) findPreference("uploads");
        final SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext());
        int uploads = sharedPref.getInt(Prefs.UPLOADS_SHOWING, 100);
        uploadLimit.setText(uploads + "");
        uploadLimit.setSummary(uploads + "");
        uploadLimit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.parseInt(newValue.toString());
                final SharedPreferences.Editor editor = sharedPref.edit();
                if (value > 500) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.maximum_limit)
                            .setMessage(R.string.maximum_limit_alert)
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    editor.putInt(Prefs.UPLOADS_SHOWING, 500);
                    editor.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED,true);
                    uploadLimit.setSummary(500 + "");
                    uploadLimit.setText(500 + "");
                } else {
                    editor.putInt(Prefs.UPLOADS_SHOWING, Integer.parseInt(newValue.toString()));
                    editor.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED,true);
                    uploadLimit.setSummary(newValue.toString());
                }
                editor.apply();
                return true;
            }

        });


    }
}