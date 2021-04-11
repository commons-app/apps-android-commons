package fr.free.nrw.commons.settings;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import androidx.preference.EditTextPreference;
import android.view.View;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.snackbar.Snackbar;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.TelemetryDefinition;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.upload.Language;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import org.wikipedia.language.AppLanguageLookUpTable;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;

    @Inject
    CommonsLogSender commonsLogSender;

    private ListPreference themeListPreference;
    private ListPreference langListPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        ApplicationlessInjection
            .getInstance(getActivity().getApplicationContext())
            .getCommonsApplicationComponent()
            .inject(this);

        // Set the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        themeListPreference = findPreference(Prefs.KEY_THEME_VALUE);
        prepareTheme();

        MultiSelectListPreference multiSelectListPref = findPreference(Prefs.MANAGED_EXIF_TAGS);
        if (multiSelectListPref != null) {
            multiSelectListPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue instanceof HashSet && !((HashSet) newValue).contains(getString(R.string.exif_tag_location))) {
                    defaultKvStore.putBoolean("has_user_manually_removed_location", true);
                }
                return true;
            });
        }

        langListPreference = findPreference("descriptionDefaultLanguagePref");
        prepareLanguages();
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
        // Disable some settings when not logged in.
        if (defaultKvStore.getBoolean("login_skipped", false)) {
            findPreference("useExternalStorage").setEnabled(false);
            findPreference("useAuthorName").setEnabled(false);
            findPreference("displayNearbyCardView").setEnabled(false);
            findPreference("descriptionDefaultLanguagePref").setEnabled(false);
            findPreference("displayLocationPermissionForCardView").setEnabled(false);
            findPreference("displayCampaignsCardView").setEnabled(false);
        }

        findPreference("telemetryOptOut").setOnPreferenceChangeListener(
            (preference, newValue) -> {
                telemetryOptInOut((boolean)newValue);
                defaultKvStore.putBoolean(Prefs.TELEMETRY_PREFERENCE,(boolean)newValue);
                return true;
            });
    }

    /**
     * Opt in or out of MapBox telemetry
     * @param shouldOptIn
     */
    private void telemetryOptInOut(boolean shouldOptIn){
        TelemetryDefinition telemetry = Mapbox.getTelemetry();
        if (telemetry != null) {
            telemetry.setUserTelemetryRequestState(shouldOptIn);
        }
    }

    @Override
    protected Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen) {
            @Override
            public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                Preference preference = getItem(position);
                View iconFrame = holder.itemView.findViewById(R.id.icon_frame);
                if (iconFrame != null) {
                    iconFrame.setVisibility(View.GONE);
                }
            }
        };
    }

    /**
     * Sets the theme pref
     */
    private void prepareTheme() {
        themeListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getActivity().recreate();
            return true;
        });
    }

    /**
     * Prepares language summary and language codes list and adds them to list preference as pairs.
     * Uses previously saved language if there is any, if not uses phone local as initial language.
     * Adds preference changed listener and saves value chosen by user to shared preferences
     * to remember later
     */
    private void prepareLanguages() {
        List<String> languageNamesList;
        List<String> languageCodesList;
        AppLanguageLookUpTable appLanguageLookUpTable = new AppLanguageLookUpTable(getContext());
        languageNamesList = appLanguageLookUpTable.getLocalizedNames();
        languageCodesList = appLanguageLookUpTable.getCodes();
        List<String> languageNameWithCodeList = new ArrayList<>();

        for (int i = 0; i < languageNamesList.size(); i++) {
            languageNameWithCodeList.add(languageNamesList.get(i) + "[" + languageCodesList.get(i) + "]");
        }

        CharSequence[] languageNames = languageNameWithCodeList.toArray(new CharSequence[0]);
        CharSequence[] languageCodes = languageCodesList.toArray(new CharSequence[0]);
        // Add all languages and languages codes to lists preference as pair
        langListPreference.setEntries(languageNames);
        langListPreference.setEntryValues(languageCodes);

        // Gets current language code from shared preferences
        String languageCode = getCurrentLanguageCode();
        if (languageCode.equals("")) {
            // If current language code is empty, means none selected by user yet so use phone local
            langListPreference.setValue(Locale.getDefault().getLanguage());
        } else {
            // If any language is selected by user previously, use it
            langListPreference.setValue(languageCode);
        }

        langListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String userSelectedValue = (String) newValue;
            saveLanguageValue(userSelectedValue);
            return true;
        });
    }

    private void saveLanguageValue(String userSelectedValue) {
        defaultKvStore.putString(Prefs.KEY_LANGUAGE_VALUE, userSelectedValue);
    }

    private String getCurrentLanguageCode() {
        return defaultKvStore.getString(Prefs.KEY_LANGUAGE_VALUE, "");
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
