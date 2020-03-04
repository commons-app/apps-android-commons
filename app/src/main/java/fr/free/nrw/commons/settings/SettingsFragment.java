package fr.free.nrw.commons.settings;

import android.Manifest;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.Editable;
import android.text.TextWatcher;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.upload.Language;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;

public class SettingsFragment extends PreferenceFragment {

    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;
    @Inject
    CommonsLogSender commonsLogSender;

    private ListPreference themeListPreference;
    private ListPreference langListPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationlessInjection
                .getInstance(getActivity().getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        themeListPreference = (ListPreference) findPreference("themePref");
        prepareTheme();

        //Check if the Author Name switch is enabled and appropriately handle the author name usage
        SwitchPreference useAuthorName = (SwitchPreference) findPreference("useAuthorName");
        EditTextPreference authorName = (EditTextPreference) findPreference("authorName");
        authorName.setEnabled(defaultKvStore.getBoolean("useAuthorName", false));
        useAuthorName.setOnPreferenceChangeListener((preference, newValue) -> {
            authorName.setEnabled((Boolean)newValue);
            return true;
        });

        MultiSelectListPreference multiSelectListPref = (MultiSelectListPreference) findPreference(Prefs.MANAGED_EXIF_TAGS);
        if (multiSelectListPref != null) {
            multiSelectListPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue instanceof HashSet && !((HashSet) newValue).contains(getString(R.string.exif_tag_location))) {
                    defaultKvStore.putBoolean("has_user_manually_removed_location", true);
                }
                return true;
            });
        }

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

        langListPreference = (ListPreference) findPreference("descriptionDefaultLanguagePref");
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
        if (defaultKvStore.getBoolean("login_skipped", false)){
            SwitchPreference useExternalStorage = (SwitchPreference) findPreference("useExternalStorage");
            SwitchPreference displayNearbyCardView = (SwitchPreference) findPreference("displayNearbyCardView");
            SwitchPreference displayLocationPermissionForCardView = (SwitchPreference) findPreference("displayLocationPermissionForCardView");
            SwitchPreference displayCampaignsCardView = (SwitchPreference) findPreference("displayCampaignsCardView");
            useExternalStorage.setEnabled(false);
            uploadLimit.setEnabled(false);
            useAuthorName.setEnabled(false);
            displayNearbyCardView.setEnabled(false);
            displayLocationPermissionForCardView.setEnabled(false);
            displayCampaignsCardView.setEnabled(false);
        }
    }

    // Return true is system wide dark theme is enabled else false
    public boolean getSystemDefaultThemeBool(String theme) {
        switch (theme) {
            case "Dark":
                return true;
            case "Default":
                return getSystemDefaultThemeBool(getSystemDefaultTheme());
            default:
                return false;
        }
    }

    // Returns the default system wide theme
    public String getSystemDefaultTheme() {
        if ((getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            return "Dark";
        }
        return "Light";
    }

    /**
     * Prepares themes list and adds them to list preference as pairs.
     * Uses previously saved theme if there is any, if not then uses default.
     * Adds preference theme listener and saves value choosen by user to shared preferences
     * to remember later
     */
    private void prepareTheme() {

        String[] themeOptionsNames = {"Default", "Dark", "Light"};

        themeListPreference.setEntries(themeOptionsNames);
        themeListPreference.setEntryValues(themeOptionsNames);

        String currentTheme = getCurrentTheme();
        if (currentTheme.equals("")){
            // If current theme code is empty, means none selected by user yet so use default
            themeListPreference.setSummary(themeOptionsNames[0]);
            themeListPreference.setValue(themeOptionsNames[0]);
        } else {
            // If any theme is selected by user previously, use it
            int prefIndex = themeListPreference.findIndexOfValue(currentTheme);
            themeListPreference.setSummary(themeListPreference.getEntries()[prefIndex]);
            themeListPreference.setValue(currentTheme);
        }

        themeListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String userSelectedValue = (String) newValue;
            int prefIndex = themeListPreference.findIndexOfValue(userSelectedValue);
            themeListPreference.setSummary(themeListPreference.getEntries()[prefIndex]);

            saveThemeValue(userSelectedValue);
            getActivity().recreate();
            return true;
        });
    }

    private void saveThemeValue(String userSelectedValue) {
        defaultKvStore.putString(Prefs.KEY_THEME_VALUE, userSelectedValue);
    }

    private String getCurrentTheme() {
        return defaultKvStore.getString(Prefs.KEY_THEME_VALUE, "Default");
    }

    /**
     * Prepares language summary and language codes list and adds them to list preference as pairs.
     * Uses previously saved language if there is any, if not uses phone local as initial language.
     * Adds preference changed listener and saves value choosen by user to shared preferences
     * to remember later
     */
    private void prepareLanguages() {
        List<String> languageNamesList = new ArrayList<>();
        List<String> languageCodesList = new ArrayList<>();
        List<Language> languages = getLanguagesSupportedByDevice();

        for(Language language: languages) {
            // Go through all languages and add them to lists
            if(!languageCodesList.contains(language.getLocale().getLanguage())) {
                // This if prevents us from adding same language twice
                languageNamesList.add(language.getLocale().getDisplayName());
                languageCodesList.add(language.getLocale().getLanguage());
            }
        }

        CharSequence[] languageNames = languageNamesList.toArray(new CharSequence[0]);
        CharSequence[] languageCodes = languageCodesList.toArray(new CharSequence[0]);
        // Add all languages and languages codes to lists preference as pair
        langListPreference.setEntries(languageNames);
        langListPreference.setEntryValues(languageCodes);

        // Gets current language code from shared preferences
        String languageCode = getCurrentLanguageCode();
        if (languageCode.equals("")){
            // If current language code is empty, means none selected by user yet so use phone local
            langListPreference.setSummary(Locale.getDefault().getDisplayLanguage());
            langListPreference.setValue(Locale.getDefault().getLanguage());
        } else {
            // If any language is selected by user previously, use it
            int prefIndex = langListPreference.findIndexOfValue(languageCode);
            langListPreference.setSummary(langListPreference.getEntries()[prefIndex]);
            langListPreference.setValue(languageCode);
        }

        langListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String userSelectedValue = (String) newValue;
            int prefIndex = langListPreference.findIndexOfValue(userSelectedValue);
            langListPreference.setSummary(langListPreference.getEntries()[prefIndex]);
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

    private List<Language> getLanguagesSupportedByDevice() {
        List<Language> languages = new ArrayList<>();
        Locale[] localesArray = Locale.getAvailableLocales();
        for (Locale locale : localesArray) {
            languages.add(new Language(locale));
        }

        Collections.sort(languages, (language, t1) -> language.getLocale().getDisplayName()
                .compareTo(t1.getLocale().getDisplayName()));
        return languages;
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
