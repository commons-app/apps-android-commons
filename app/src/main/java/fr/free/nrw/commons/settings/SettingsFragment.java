package fr.free.nrw.commons.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.utils.FileUtils;

public class SettingsFragment extends PreferenceFragment {

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 100;

    @Inject @Named("default_preferences") SharedPreferences prefs;

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

        //initialize the emailSelectorIntent
        Intent emailSelectorIntent = new Intent(Intent.ACTION_SENDTO);
        emailSelectorIntent.setData(Uri.parse("mailto:"));
        //initialize the emailIntent
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // Logs must be sent to the PRIVATE email. Please do not modify this without good reason!
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{CommonsApplication.LOGS_PRIVATE_EMAIL});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(CommonsApplication.LOGS_PRIVATE_EMAIL_SUBJECT, BuildConfig.VERSION_NAME));
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        emailIntent.setSelector( emailSelectorIntent );
        //adding the attachment to the intent
        emailIntent.putExtra(Intent.EXTRA_STREAM, appLogsFilePath);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail.."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
        }
    }

}
