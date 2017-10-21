package fr.free.nrw.commons.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.utils.FileUtils;

public class SettingsFragment extends PreferenceFragment {

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 100;

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

        Preference sendLogsPreference = findPreference("sendLogFile");
        sendLogsPreference.setOnPreferenceClickListener(preference -> {
            //first we need to check if we have the necessary permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        ==
                        PackageManager.PERMISSION_GRANTED) {
                    sendAppLogsViaEmail();
                } else {
                    //first get the necessary permission
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                }
            } else {
                sendAppLogsViaEmail();
            }

            return true;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendAppLogsViaEmail();
            }
        }
    }

    private void sendAppLogsViaEmail() {
        String appLogs = Utils.getAppLogs();
        File appLogsFile = FileUtils.createAndGetAppLogsFile(appLogs);

        Context applicationContext = getActivity().getApplicationContext();
        Uri appLogsFilePath = FileProvider.getUriForFile(
                getActivity(),
                applicationContext.getPackageName() + ".provider",
                appLogsFile
        );

        Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
        feedbackIntent.setType("message/rfc822");
        feedbackIntent.putExtra(Intent.EXTRA_EMAIL,
                new String[]{CommonsApplication.FEEDBACK_EMAIL});
        feedbackIntent.putExtra(Intent.EXTRA_SUBJECT,
                String.format(CommonsApplication.FEEDBACK_EMAIL_SUBJECT,
                        BuildConfig.VERSION_NAME));
        feedbackIntent.putExtra(Intent.EXTRA_STREAM,appLogsFilePath);

        try {
            startActivity(feedbackIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
        }
    }
}
