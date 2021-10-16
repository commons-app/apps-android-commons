package fr.free.nrw.commons.settings;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
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
import fr.free.nrw.commons.campaigns.CampaignView;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
    private ListPreference descriptionLanguageListPreference;
    private ListPreference appUiLanguageListPreference;
    private String keyLanguageListPreference;

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

        appUiLanguageListPreference = findPreference("appUiDefaultLanguagePref");
        assert appUiLanguageListPreference != null;
        keyLanguageListPreference = appUiLanguageListPreference.getKey();
        prepareAppLanguages(keyLanguageListPreference);

        descriptionLanguageListPreference = findPreference("descriptionDefaultLanguagePref");
        assert descriptionLanguageListPreference != null;
        keyLanguageListPreference = descriptionLanguageListPreference.getKey();
        prepareAppLanguages(keyLanguageListPreference);

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
            findPreference(CampaignView.CAMPAIGNS_DEFAULT_PREFERENCE).setEnabled(false);
            findPreference("managed_exif_tags").setEnabled(false);
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
    protected Adapter onCreateAdapter(final PreferenceScreen preferenceScreen) {
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
     * Uses previously saved language if there is any, if not uses phone locale as initial language.
     * Get ListPreference key and act accordingly for each ListPreference.
     * Adds preference changed listener and saves value chosen by user to shared preferences
     * to remember later and recall MainActivity to reflect language changes
     * @param keyListPreference
     */
    private void prepareAppLanguages(final String keyListPreference) {
        final List<String> languageNamesList;
        final List<String> languageCodesList;
        final AppLanguageLookUpTable appLanguageLookUpTable = new AppLanguageLookUpTable(
            Objects.requireNonNull(getContext()));
        languageNamesList = appLanguageLookUpTable.getLocalizedNames();
        languageCodesList = appLanguageLookUpTable.getCodes();
        List<String> languageNameWithCodeList = new ArrayList<>();

        for (int i = 0; i < languageNamesList.size(); i++) {
            languageNameWithCodeList.add(languageNamesList.get(i) + "[" + languageCodesList.get(i) + "]");
        }

        final CharSequence[] languageNames = languageNamesList.toArray(new CharSequence[0]);
        final CharSequence[] languageCodes = languageCodesList.toArray(new CharSequence[0]);
        // Add all languages and languages codes to lists preference as pair

        // Gets current language code from shared preferences
        final String languageCode = getCurrentLanguageCode(keyListPreference);

        if (keyListPreference.equals("appUiDefaultLanguagePref")) {
            appUiLanguageListPreference.setEntries(languageNames);
            appUiLanguageListPreference.setEntryValues(languageCodes);

            assert languageCode != null;
            if (languageCode.equals("")) {
                // If current language code is empty, means none selected by user yet so use phone local
                appUiLanguageListPreference.setValue(Locale.getDefault().getLanguage());
            } else {
                // If any language is selected by user previously, use it
                appUiLanguageListPreference.setValue(languageCode);
            }

            appUiLanguageListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                final String userSelectedValue = (String) newValue;
                setLocale(Objects.requireNonNull(getActivity()), userSelectedValue);
                saveLanguageValue(userSelectedValue, keyListPreference);
                getActivity().recreate();
                final Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                return true;
            });

        } else if (keyListPreference.equals("descriptionDefaultLanguagePref")) {
            descriptionLanguageListPreference.setEntries(languageNames);
            descriptionLanguageListPreference.setEntryValues(languageCodes);

            assert languageCode != null;
            if (languageCode.equals("")) {
                // If current language code is empty, means none selected by user yet so use phone local
                descriptionLanguageListPreference.setValue(Locale.getDefault().getLanguage());
            } else {
                // If any language is selected by user previously, use it
                descriptionLanguageListPreference.setValue(languageCode);
            }

            descriptionLanguageListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                final String userSelectedValue = (String) newValue;
                saveLanguageValue(userSelectedValue, keyListPreference);
                return true;
            });
        }
    }

    /**
     * Changing the default app language with selected one and save it to SharedPreferences
     */
    public void setLocale(final Activity activity, String userSelectedValue) {
        if (userSelectedValue.equals("")) {
            userSelectedValue = Locale.getDefault().getLanguage();
        }
        final Locale locale = new Locale(userSelectedValue);
        Locale.setDefault(locale);
        final Configuration configuration = new Configuration();
        configuration.locale = locale;
        activity.getBaseContext().getResources().updateConfiguration(configuration,
            activity.getBaseContext().getResources().getDisplayMetrics());

        final SharedPreferences.Editor editor = activity.getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("language", userSelectedValue);
        editor.apply();
    }

    /**
     * Save userselected language in List Preference
     * @param userSelectedValue
     * @param preferenceKey
     */
    private void saveLanguageValue(final String userSelectedValue, final String preferenceKey) {
        if (preferenceKey.equals("appUiDefaultLanguagePref")) {
            defaultKvStore.putString(Prefs.APP_UI_LANGUAGE, userSelectedValue);
        } else if (preferenceKey.equals("descriptionDefaultLanguagePref")) {
            defaultKvStore.putString(Prefs.DESCRIPTION_LANGUAGE, userSelectedValue);
        }
    }

    /**
     * Gets current language code from shared preferences
     * @param preferenceKey
     * @return
     */
    private String getCurrentLanguageCode(final String preferenceKey) {
        if (preferenceKey.equals("appUiDefaultLanguagePref")) {
            return defaultKvStore.getString(Prefs.APP_UI_LANGUAGE, "");
        }
        if (preferenceKey.equals("descriptionDefaultLanguagePref")) {
            return defaultKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, "");
        }
        return null;
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
