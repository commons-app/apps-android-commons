package fr.free.nrw.commons.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.Editable;
import android.text.TextWatcher;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;

import static android.R.*;

public class SettingsFragment extends PreferenceFragment {

    @Inject
    @Named("default_preferences")
    BasicKvStore defaultKvStore;
    @Inject
    CommonsLogSender commonsLogSender;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationlessInjection
                .getInstance(getActivity().getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        SwitchPreference themePreference = (SwitchPreference) findPreference("theme");
        themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getActivity().recreate();
            return true;
        });

        //Check if the Author Name switch is enabled and appropriately handle the author name usage
        SwitchPreference useAuthorName = (SwitchPreference) findPreference("useAuthorName");
        EditTextPreference authorName = (EditTextPreference) findPreference("authorName");
        authorName.setEnabled(defaultKvStore.getBoolean("useAuthorName", false));
        useAuthorName.setOnPreferenceChangeListener((preference, newValue) -> {
            authorName.setEnabled((Boolean)newValue);
            return true;
        });

        final EditTextPreference uploadLimit = (EditTextPreference) findPreference("uploads");
        int uploads = defaultKvStore.getInt(Prefs.UPLOADS_SHOWING, 100);
        uploadLimit.setText(uploads + "");
        uploadLimit.setSummary(uploads + "");
        uploadLimit.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int value;
                if (s.length()>0)
                try {
                    value = Integer.parseInt(s.toString());
                    if (value > 500) {
                        uploadLimit.getEditText().setError(getString((R.string.maximum_limit_alert)));
                        defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, 500);
                        defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COU NT_CHANGED, true);
                        uploadLimit.setSummary(500 + "");
                        uploadLimit.setText(500 + "");
                    } else if (value == 0) {
                        uploadLimit.getEditText().setError(getString(R.string.cannot_be_zero));
                        defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, 100);
                        defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                        uploadLimit.setSummary(100 + "");
                        uploadLimit.setText(100 + "");
                    } else {
                        defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, value);
                        defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                        uploadLimit.setSummary(String.valueOf(value));
                    }
                } catch (Exception e) {
                    uploadLimit.getEditText().setError(getString(R.string.enter_valid));
                    defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, 100);
                    defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                    uploadLimit.setSummary(100 + "");
                    uploadLimit.setText(100 + "");
                }
            }
        });
        /*uploadLimit.setOnPreferenceChangeListener((preference, newValue) -> {
            int value;
            try {
                value = Integer.parseInt(newValue.toString());
                if (value > 500) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.maximum_limit)
                            .setMessage(R.string.maximum_limit_alert)
                            .setPositiveButton(string.yes, (dialog, which) -> {
                            })
                            .setIcon(drawable.ic_dialog_alert)
                            .show();
                    defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, 500);
                    defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                    uploadLimit.setSummary(500 + "");
                    uploadLimit.setText(500 + "");
                } else if (value == 0) {
                    uploadLimit.getEditText().setError(getString(R.string.cannot_be_zero));
                    *//*new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.invalid_zero)
                            .setMessage(R.string.cannot_be_zero)
                            .setPositiveButton(string.yes, (dialog, which) -> {
                            })
                            .setIcon(drawable.ic_dialog_alert)
                            .show();*//*
                    *//*defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, 100);
                    defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                    uploadLimit.setSummary(100 + "");
                    uploadLimit.setText(100 + "");*//*
                    return false;
                } else {
                    defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, value);
                    defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                    uploadLimit.setSummary(String.valueOf(value));
                }
            } catch (Exception e) {
                uploadLimit.getEditText().setError(getString(R.string.enter_valid));
                *//*new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.invalid_input)
                        .setMessage(R.string.enter_valid)
                        .setPositiveButton(string.yes, (dialog, which) -> {
                        })
                        .setIcon(drawable.ic_dialog_alert)
                        .show();*//*
                defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, 100);
                defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                uploadLimit.setSummary(100 + "");
                uploadLimit.setText(100 + "");
            }
            return false;
        });
*/
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
