package fr.free.nrw.commons.settings;

import android.Manifest;
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
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;

public class SettingsFragment extends PreferenceFragment {

    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;
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
        int currentUploadLimit = defaultKvStore.getInt(Prefs.UPLOADS_SHOWING, 100);
        uploadLimit.setText(Integer.toString(currentUploadLimit));
        uploadLimit.setSummary(Integer.toString(currentUploadLimit));
        uploadLimit.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) return;

                int value = Integer.parseInt(s.toString());

                if (value > 500) {
                    uploadLimit.getEditText().setError(getString(R.string.maximum_limit_alert));
                    value = 500;
                } else if (value == 0) {
                    uploadLimit.getEditText().setError(getString(R.string.cannot_be_zero));
                    value = 100;
                }

                defaultKvStore.putInt(Prefs.UPLOADS_SHOWING, value);
                defaultKvStore.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, true);
                uploadLimit.setText(Integer.toString(value));
                uploadLimit.setSummary(Integer.toString(value));
            }
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
        // Disable "Recent upload limit" and "Use custom author name" when not logged in.
        if (defaultKvStore.getBoolean("login_skipped", false)){
            SwitchPreference useExternalStorage = (SwitchPreference) findPreference("useExternalStorage");
            useExternalStorage.setEnabled(false);
            uploadLimit.setEnabled(false);
            useAuthorName.setEnabled(false);
        }
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
