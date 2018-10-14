package fr.free.nrw.commons.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;

public class SettingsFragment extends PreferenceFragment {

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 100;

    @Inject @Named("default_preferences") SharedPreferences prefs;
    @Inject CommonsLogSender commonsLogSender;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationlessInjection
                .getInstance(getActivity().getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

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

        SwitchPreference themePreference = (SwitchPreference) findPreference("theme");
        themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getActivity().recreate();
            return true;
        });

        final EditTextPreference uploadLimit = (EditTextPreference) findPreference("uploads");
        int uploads = prefs.getInt(Prefs.UPLOADS_SHOWING, 100);
        uploadLimit.setText(uploads + "");
        uploadLimit.setSummary(uploads + "");
        uploadLimit.setOnPreferenceChangeListener((preference, newValue) -> {
            int value;
            try {
                value = Integer.parseInt(newValue.toString());
            } catch(Exception e) {
                value = 100; //Default number
            }
            final SharedPreferences.Editor editor = prefs.edit();
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
                editor.putInt(Prefs.UPLOADS_SHOWING, value);
                editor.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED,true);
                uploadLimit.setSummary(String.valueOf(value));
            }
            editor.apply();
            return true;
        });

        Preference betaTesterPreference = findPreference("becomeBetaTester");
        betaTesterPreference.setOnPreferenceClickListener(preference -> {
            Utils.handleWebUrl(getActivity(), Uri.parse(getResources().getString(R.string.beta_opt_in_link)));
            return true;
        });
        Preference sendLogsPreference = findPreference("sendLogFile");
        sendLogsPreference.setOnPreferenceClickListener(preference -> {
            checkPermissionsAndSendLogs();
            return true;
        });
    }

    /**
     * First checks for external storage permissions and then sends logs via email
     */
    private void checkPermissionsAndSendLogs() {
        if (PermissionUtils.hasPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            commonsLogSender.send(getActivity(), null);
        } else {
            requestExternalStoragePermissions();
        }
    }

    /**
     * Requests external storage permissions and shows a toast stating that log collection has started
     */
    private void requestExternalStoragePermissions() {
        Dexter.withActivity(getActivity())
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new BasePermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.log_collection_started));
                    }
                }).check();
    }
}
