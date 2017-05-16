package fr.free.nrw.commons.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.utils.CommonsAppSharedPref;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Update spinner to show selected value as summary
        ListPreference licensePreference = (ListPreference) findPreference(Prefs.DEFAULT_LICENSE);
        licensePreference.setSummary(getString(Utils.licenseNameFor(licensePreference.getValue())));

        // Keep summary updated when changing value
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

        final EditTextPreference uploadLimit = (EditTextPreference) findPreference("uploads");
        final CommonsAppSharedPref commonsAppSharedPref = CommonsAppSharedPref
                .getInstance(getActivity());
        int uploads = commonsAppSharedPref.getPreferenceInt(Prefs.UPLOADS_SHOWING, 100);
        uploadLimit.setText(uploads + "");
        uploadLimit.setSummary(uploads + "");
        uploadLimit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.parseInt(newValue.toString());
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
                    commonsAppSharedPref.putPreferenceInt(Prefs.UPLOADS_SHOWING, 500);
                    commonsAppSharedPref.putPreferenceBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED,true);
                    uploadLimit.setSummary(500 + "");
                    uploadLimit.setText(500 + "");
                } else {
                    commonsAppSharedPref.putPreferenceInt(Prefs.UPLOADS_SHOWING, Integer.parseInt(newValue.toString()));
                    commonsAppSharedPref.putPreferenceBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED,true);
                    uploadLimit.setSummary(newValue.toString());
                }
                return true;
            }

        });


    }
}
