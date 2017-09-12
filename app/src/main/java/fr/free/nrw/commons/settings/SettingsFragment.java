package fr.free.nrw.commons.settings;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import fr.free.nrw.commons.CommonsApplication;
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
        licensePreference.setSummary(getString(Utils.licenseNameFor(licensePreference.getValue())));

        // Keep summary updated when changing value
        licensePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(getString(Utils.licenseNameFor((String) newValue)));
            return true;
        });

        CheckBoxPreference themePreference = (CheckBoxPreference) findPreference("theme");
        themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getActivity().recreate();
            return true;
        });

        final EditTextPreference uploadLimit = (EditTextPreference) findPreference("uploads");
        final SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(CommonsApplication.getInstance());
        int uploads = sharedPref.getInt(Prefs.UPLOADS_SHOWING, 100);
        uploadLimit.setText(uploads + "");
        uploadLimit.setSummary(uploads + "");
        uploadLimit.setOnPreferenceChangeListener((preference, newValue) -> {
            int value = Integer.parseInt(newValue.toString());
            final SharedPreferences.Editor editor = sharedPref.edit();
            if (value > 500) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.maximum_limit)
                        .setMessage(R.string.maximum_limit_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {})
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
        });


    }
}
