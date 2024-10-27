package fr.free.nrw.commons.settings;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest.permission;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.campaigns.CampaignView;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.recentlanguages.Language;
import fr.free.nrw.commons.recentlanguages.RecentLanguagesAdapter;
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao;
import fr.free.nrw.commons.recentlanguages.SavedLanguagesAdapter;
import fr.free.nrw.commons.upload.LanguagesAdapter;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;

    @Inject
    CommonsLogSender commonsLogSender;

    @Inject
    RecentLanguagesDao recentLanguagesDao;

    @Inject
    ContributionController contributionController;

    @Inject
    LocationServiceManager locationManager;

    private ListPreference themeListPreference;
    private Preference descriptionLanguageListPreference;
    private Preference descriptionSecondaryLanguagesListPreference;
    private Preference appUiLanguageListPreference;
    private String keyLanguageListPreference;
    private TextView recentLanguagesTextView;
    private View separator;
    private ListView languageHistoryListView;
    private static final String GET_CONTENT_PICKER_HELP_URL = "https://commons-app.github.io/docs.html#get-content";

    private final ActivityResultLauncher<Intent> cameraPickLauncherForResult =
        registerForActivityResult(new StartActivityForResult(),
        result -> {
            contributionController.handleActivityResultWithCallback(requireActivity(), callbacks -> {
                contributionController.onPictureReturnedFromCamera(result, requireActivity(), callbacks);
            });
        });

    private ActivityResultLauncher<String[]> inAppCameraLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean areAllGranted = true;
            for (final boolean b : result.values()) {
                areAllGranted = areAllGranted && b;
            }
            if (!areAllGranted && shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                contributionController.handleShowRationaleFlowCameraLocation(getActivity(), inAppCameraLocationPermissionLauncher, cameraPickLauncherForResult);
            }
        }
    });

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

        Preference inAppCameraLocationPref = findPreference("inAppCameraLocationPref");

        inAppCameraLocationPref.setOnPreferenceChangeListener(
            (preference, newValue) -> {
                boolean isInAppCameraLocationTurnedOn = (boolean) newValue;
                if (isInAppCameraLocationTurnedOn) {
                    createDialogsAndHandleLocationPermissions(getActivity());
                }
                return true;
            }
        );

        // Gets current language code from shared preferences
        String languageCode;

        appUiLanguageListPreference = findPreference("appUiDefaultLanguagePref");
        assert appUiLanguageListPreference != null;
        keyLanguageListPreference = appUiLanguageListPreference.getKey();
        languageCode = getCurrentLanguageCode(keyLanguageListPreference);
        assert languageCode != null;
        if (languageCode.equals("")) {
            // If current language code is empty, means none selected by user yet so use phone local
            appUiLanguageListPreference.setSummary(Locale.getDefault().getDisplayLanguage());
        } else {
            // If any language is selected by user previously, use it
            Locale defLocale = createLocale(languageCode);
            appUiLanguageListPreference.setSummary((defLocale).getDisplayLanguage(defLocale));
        }
        appUiLanguageListPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prepareAppLanguages(appUiLanguageListPreference.getKey());
                return true;
            }
        });

        descriptionLanguageListPreference = findPreference("descriptionDefaultLanguagePref");
        assert descriptionLanguageListPreference != null;
        keyLanguageListPreference = descriptionLanguageListPreference.getKey();
        languageCode = getCurrentLanguageCode(keyLanguageListPreference);
        assert languageCode != null;
        if (languageCode.equals("")) {
            // If current language code is empty, means none selected by user yet so use phone local
            descriptionLanguageListPreference.setSummary(Locale.getDefault().getDisplayLanguage());
        } else {
            // If any language is selected by user previously, use it
            Locale defLocale = createLocale(languageCode);
            descriptionLanguageListPreference.setSummary(defLocale.getDisplayLanguage(defLocale));
        }
        descriptionLanguageListPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prepareAppLanguages(descriptionLanguageListPreference.getKey());
                return true;
            }
        });


        descriptionSecondaryLanguagesListPreference = findPreference("descriptionSecondaryLanguagesPref");
        assert descriptionSecondaryLanguagesListPreference != null;
        keyLanguageListPreference = descriptionSecondaryLanguagesListPreference.getKey();
        languageCode = getCurrentLanguageCode(keyLanguageListPreference);
        assert languageCode != null;
        descriptionSecondaryLanguagesListPreference.setSummary("List additional languages.");

        descriptionSecondaryLanguagesListPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prepareSecondaryLanguagesDialog();
                return true;
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

        Preference documentBasedPickerPreference = findPreference("openDocumentPhotoPickerPref");
        documentBasedPickerPreference.setOnPreferenceChangeListener(
            (preference, newValue) -> {
                boolean isGetContentPickerTurnedOn = !(boolean) newValue;
                if (isGetContentPickerTurnedOn) {
                    showLocationLossWarning();
                }
                return true;
            }
        );
        // Disable some settings when not logged in.
        if (defaultKvStore.getBoolean("login_skipped", false)) {
            findPreference("useExternalStorage").setEnabled(false);
            findPreference("useAuthorName").setEnabled(false);
            findPreference("displayNearbyCardView").setEnabled(false);
            findPreference("descriptionDefaultLanguagePref").setEnabled(false);
            findPreference("descriptionSecondaryLanguagesPref").setEnabled(false);
            findPreference("displayLocationPermissionForCardView").setEnabled(false);
            findPreference(CampaignView.CAMPAIGNS_DEFAULT_PREFERENCE).setEnabled(false);
            findPreference("managed_exif_tags").setEnabled(false);
            findPreference("openDocumentPhotoPickerPref").setEnabled(false);
            findPreference("inAppCameraLocationPref").setEnabled(false);
        }
    }

    /**
     * Asks users to provide location access
     *
     * @param activity
     */
    private void createDialogsAndHandleLocationPermissions(Activity activity) {
        inAppCameraLocationPermissionLauncher.launch(new String[]{permission.ACCESS_FINE_LOCATION});
    }

    /**
     * On some devices, the new Photo Picker with GET_CONTENT takeover
     * redacts location tags from EXIF metadata
     *
     * Show warning to the user when ACTION_GET_CONTENT intent is enabled
     */
    private void showLocationLossWarning() {
        DialogUtil.showAlertDialog(
            getActivity(),
            null,
            getString(R.string.location_loss_warning),
            getString(R.string.ok),
            getString(R.string.read_help_link),
            () -> {},
            () -> Utils.handleWebUrl(requireContext(), Uri.parse(GET_CONTENT_PICKER_HELP_URL)),
            null,
            true
        );
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
     * Updates the ListView to display saved languages using the SavedLanguagesAdapter.
     *
     * @param savedLanguageListView  The ListView that will display the saved languages.
     * @param savedLanguages         A list of saved Language objects to be displayed.
     * @param selectedLanguages      A HashMap containing the selected language IDs and their corresponding names.
     */
    private void updateSavedLanguages(ListView savedLanguageListView, List<Language> savedLanguages, HashMap<Integer, String> selectedLanguages) {
        // Use SavedLanguagesAdapter to display saved languages
        SavedLanguagesAdapter savedLanguagesAdapter = new SavedLanguagesAdapter(
            getActivity(),
            savedLanguages,  // List of saved Language objects
            selectedLanguages  // Pass the map of selected languages
        );

        // Set the adapter to the ListView to display the saved languages
        savedLanguageListView.setAdapter(savedLanguagesAdapter);
    }

    /**
     * Deserializes a comma-separated string of language codes into an ArrayList of strings.
     *
     * @param languageCodes  A string containing language codes separated by commas.
     * @return               An ArrayList of language codes, or an empty ArrayList if the input is null or empty.
     */
    private ArrayList<String> deSerialise(String languageCodes) {
        // Check if the stored string is empty or null
        if (languageCodes == null || languageCodes.isEmpty()) {
            return new ArrayList<>();  // Return an empty list if there's no data
        }

        // Split the string by commas and store it in a list
        String[] languageArray = languageCodes.split(",\\s*");  // Split by comma and optional space
        return new ArrayList<>(Arrays.asList(languageArray));  // Convert array to ArrayList and return
    }

    /**
     * Prepare and Show language selection dialog box
     * Disable default/already selected language from dialog box
     * Saves values chosen by user to shared preferences as a serialised string.
     */
    private void prepareSecondaryLanguagesDialog() {
        final String languageCode = getCurrentLanguageCode("descriptionSecondaryLanguagesPref");
        HashMap<Integer, String> selectedLanguages = new HashMap<>();
        assert languageCode != null;
        selectedLanguages.put(0, Locale.getDefault().getLanguage());

        // Deserializing saved language codes to Language objects
        ArrayList<Language> savedLanguages = new ArrayList<>();
        for (String code : deSerialise(languageCode)) {
            if(code.equals(Locale.getDefault().getLanguage())){
                continue;
            }
            Locale locale = new Locale(code);
            savedLanguages.add(new Language(locale.getDisplayLanguage(locale), code));
        }

        // Create the new dialog for secondary language
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_select_secondary_language);
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setLayout(
            (int) (getActivity().getResources().getDisplayMetrics().widthPixels * 0.90),
            (int) (getActivity().getResources().getDisplayMetrics().heightPixels * 0.90)
        );
        dialog.show();

        // Bind UI elements
        EditText editText = dialog.findViewById(R.id.search_language);
        ListView listView = dialog.findViewById(R.id.language_list);
        ListView savedLanguageListView = dialog.findViewById(R.id.language_history_list);
        View separator = dialog.findViewById(R.id.separator);

        // Setup saved languages with the new SavedLanguagesAdapter
        updateSavedLanguages(savedLanguageListView, savedLanguages, selectedLanguages);

        // Set an onItemClickListener to remove a language when clicked
        savedLanguageListView.setOnItemClickListener((adapterView, view, position, id) -> {
            // Remove the clicked language from Saved_Languages
            savedLanguages.remove(position);

            // Update the saved language list view after removing the language
            updateSavedLanguages(savedLanguageListView, savedLanguages, selectedLanguages);

            // Update the shared preferences to reflect the removal
            String updatedLanguageCodes = "";
            for (Language language : savedLanguages) {
                updatedLanguageCodes += language.getLanguageCode() + ", ";
            }
            // Remove the trailing comma and space if present
            if (!updatedLanguageCodes.isEmpty()) {
                updatedLanguageCodes = updatedLanguageCodes.substring(0, updatedLanguageCodes.length() - 2);
            }
            saveLanguageValue(updatedLanguageCodes, "descriptionSecondaryLanguagesPref");
//            descriptionSecondaryLanguagesListPreference.setSummary(getCurrentLanguageCode("descriptionSecondaryLanguagesPref"));
        });

        // Set up the adapter for new languages using the selectedLanguages map
        LanguagesAdapter languagesAdapter = new LanguagesAdapter(getActivity(), selectedLanguages);
        listView.setAdapter(languagesAdapter);

        // Add search functionality
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                hideRecentLanguagesSection();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                languagesAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // Handle item click for language selection in the main list
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            String selectedLanguageCode = languagesAdapter.getLanguageCode(i);
            String selectedLanguageName = languagesAdapter.getLanguageName(i);

            if (deSerialise(getCurrentLanguageCode("descriptionSecondaryLanguagesPref")).contains(selectedLanguageCode)) {
                Toast.makeText(getActivity(), "Language already selected", Toast.LENGTH_SHORT).show();
                return;
            }

            savedLanguages.add(new Language(selectedLanguageName, selectedLanguageCode));
            updateSavedLanguages(savedLanguageListView, savedLanguages, selectedLanguages);

            // Update the shared preferences to reflect the addition
            String updatedLanguageCodes = "";
            for (Language language : savedLanguages) {
                updatedLanguageCodes += language.getLanguageCode() + ", ";
            }
            // Remove the trailing comma and space if present
            if (!updatedLanguageCodes.isEmpty()) {
                updatedLanguageCodes = updatedLanguageCodes.substring(0, updatedLanguageCodes.length() - 2);
            }

            saveLanguageValue(updatedLanguageCodes, "descriptionSecondaryLanguagesPref");

//            descriptionSecondaryLanguagesListPreference.setSummary(getCurrentLanguageCode("descriptionSecondaryLanguagesPref"));
        });

        dialog.setOnDismissListener(dialogInterface -> {
            // Update the shared preferences to reflect changes
            String updatedLanguageCodes = "";
            for (Language language : savedLanguages) {
                updatedLanguageCodes += language.getLanguageCode() + ", ";
            }
            // Remove the trailing comma and space if present
            if (!updatedLanguageCodes.isEmpty()) {
                updatedLanguageCodes = updatedLanguageCodes.substring(0, updatedLanguageCodes.length() - 2);
            }
            saveLanguageValue(updatedLanguageCodes, "descriptionSecondaryLanguagesPref");

        });
    }




    /**
     * Prepare and Show language selection dialog box
     * Uses previously saved language if there is any, if not uses phone locale as initial language.
     * Disable default/already selected language from dialog box
     * Get ListPreference key and act accordingly for each ListPreference.
     * saves value chosen by user to shared preferences
     * to remember later and recall MainActivity to reflect language changes
     * @param keyListPreference
     */
    private void prepareAppLanguages(final String keyListPreference) {


        // Gets current language code from shared preferences
        final String languageCode = getCurrentLanguageCode(keyListPreference);
        final List<Language> recentLanguages = recentLanguagesDao.getRecentLanguages();
        HashMap<Integer, String> selectedLanguages = new HashMap<>();

        if (keyListPreference.equals("appUiDefaultLanguagePref")) {

            assert languageCode != null;
            if (languageCode.equals("")) {
                selectedLanguages.put(0, Locale.getDefault().getLanguage());
            } else {
                selectedLanguages.put(0, languageCode);
            }
        } else if (keyListPreference.equals("descriptionDefaultLanguagePref")) {

            assert languageCode != null;
            if (languageCode.equals("")) {
                selectedLanguages.put(0, Locale.getDefault().getLanguage());

            } else {
                selectedLanguages.put(0, languageCode);
            }
        } else if (keyListPreference.equals("descriptionSecondaryLanguagesPref")) {

        assert languageCode != null;
        if (languageCode.equals("")) {
            selectedLanguages.put(0, Locale.getDefault().getLanguage());

        } else {
            selectedLanguages.put(0, languageCode);
        }
    }

        LanguagesAdapter languagesAdapter = new LanguagesAdapter(
            getActivity(),
            selectedLanguages
        );

        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_select_language);

        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setLayout((int)(getActivity().getResources().getDisplayMetrics().widthPixels*0.90),
            (int)(getActivity().getResources().getDisplayMetrics().heightPixels*0.90));
        dialog.show();

        EditText editText = dialog.findViewById(R.id.search_language);
        ListView listView = dialog.findViewById(R.id.language_list);
        languageHistoryListView = dialog.findViewById(R.id.language_history_list);
        recentLanguagesTextView = dialog.findViewById(R.id.recent_searches);
        separator = dialog.findViewById(R.id.separator);

        setUpRecentLanguagesSection(recentLanguages, selectedLanguages);

        listView.setAdapter(languagesAdapter);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                int i2) {
                hideRecentLanguagesSection();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1,
                int i2) {
                languagesAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        languageHistoryListView.setOnItemClickListener((adapterView, view, position, id) -> {
            onRecentLanguageClicked(keyListPreference, dialog, adapterView, position);
        });

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i,
                long l) {
                String languageCode = ((LanguagesAdapter) adapterView.getAdapter())
                    .getLanguageCode(i);
                final String languageName = ((LanguagesAdapter) adapterView.getAdapter())
                    .getLanguageName(i);
                final boolean isExists = recentLanguagesDao.findRecentLanguage(languageCode);
                if (isExists) {
                    recentLanguagesDao.deleteRecentLanguage(languageCode);
                }
                recentLanguagesDao.addRecentLanguage(new Language(languageName, languageCode));
                saveLanguageValue(languageCode, keyListPreference);
                Locale defLocale = createLocale(languageCode);
                if(keyListPreference.equals("appUiDefaultLanguagePref")) {
                    appUiLanguageListPreference.setSummary(defLocale.getDisplayLanguage(defLocale));
                    setLocale(requireActivity(), languageCode);
                    getActivity().recreate();
                    final Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                }else if(keyListPreference.equals("descriptionDefaultLanguagePref")){
                    descriptionLanguageListPreference.setSummary(defLocale.getDisplayLanguage(defLocale));
                }
                dialog.dismiss();
            }
        });

        dialog.setOnDismissListener(
            dialogInterface -> languagesAdapter.getFilter().filter(""));
    }

    /**
     * Set up recent languages section
     *
     * @param recentLanguages recently used languages
     * @param selectedLanguages selected languages
     */
    private void setUpRecentLanguagesSection(List<Language> recentLanguages,
        HashMap<Integer, String> selectedLanguages) {
        if (recentLanguages.isEmpty()) {
            languageHistoryListView.setVisibility(View.GONE);
            recentLanguagesTextView.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);
        } else {
            if (recentLanguages.size() > 5) {
                for (int i = recentLanguages.size()-1; i >=5; i--) {
                    recentLanguagesDao
                        .deleteRecentLanguage(recentLanguages.get(i).getLanguageCode());
                }
            }
            languageHistoryListView.setVisibility(View.VISIBLE);
            recentLanguagesTextView.setVisibility(View.VISIBLE);
            separator.setVisibility(View.VISIBLE);
            final RecentLanguagesAdapter recentLanguagesAdapter
                = new RecentLanguagesAdapter(
                getActivity(),
                recentLanguagesDao.getRecentLanguages(),
                selectedLanguages);
            languageHistoryListView.setAdapter(recentLanguagesAdapter);
        }
    }

    /**
     * Handles click event for recent language section
     */
    private void onRecentLanguageClicked(String keyListPreference, Dialog dialog, AdapterView<?> adapterView,
        int position) {
        final String recentLanguageCode = ((RecentLanguagesAdapter) adapterView.getAdapter())
            .getLanguageCode(position);
        final String recentLanguageName = ((RecentLanguagesAdapter) adapterView.getAdapter())
            .getLanguageName(position);
        final boolean isExists = recentLanguagesDao.findRecentLanguage(recentLanguageCode);
        if (isExists) {
            recentLanguagesDao.deleteRecentLanguage(recentLanguageCode);
        }
        recentLanguagesDao.addRecentLanguage(
            new Language(recentLanguageName, recentLanguageCode));
        saveLanguageValue(recentLanguageCode, keyListPreference);
        final Locale defLocale = createLocale(recentLanguageCode);
        if (keyListPreference.equals("appUiDefaultLanguagePref")) {
            appUiLanguageListPreference.setSummary(defLocale.getDisplayLanguage(defLocale));
            setLocale(requireActivity(), recentLanguageCode);
            getActivity().recreate();
            final Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        } else {
            descriptionLanguageListPreference.setSummary(defLocale.getDisplayLanguage(defLocale));
        }
        dialog.dismiss();
    }

    /**
     * Remove the section of recent languages
     */
    private void hideRecentLanguagesSection() {
        languageHistoryListView.setVisibility(View.GONE);
        recentLanguagesTextView.setVisibility(View.GONE);
        separator.setVisibility(View.GONE);
    }

    /**
     * Changing the default app language with selected one and save it to SharedPreferences
     */
    public void setLocale(final Activity activity, String userSelectedValue) {

        final Locale locale = createLocale(userSelectedValue);
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
     * Create Locale based on different types of language codes
     * @param languageCode
     * @return Locale and throws error for invalid language codes
     */
    public static Locale createLocale(String languageCode) {
        String[] parts = languageCode.split("-");
        switch (parts.length) {
            case 1:
                return new Locale(parts[0]);
            case 2:
                return new Locale(parts[0], parts[1]);
            case 3:
                return new Locale(parts[0], parts[1], parts[2]);
            default:
                throw new IllegalArgumentException("Invalid language code: " + languageCode);
        }
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
        } else if (preferenceKey.equals("descriptionSecondaryLanguagesPref")) {
            defaultKvStore.putString(Prefs.SECONDARY_LANGUAGES, userSelectedValue);
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
        if (preferenceKey.equals("descriptionSecondaryLanguagesPref")) {
            return defaultKvStore.getString(Prefs.SECONDARY_LANGUAGES, "");
        }
        return null;
    }

    /**
     * First checks for external storage permissions and then sends logs via email
     */
    private void checkPermissionsAndSendLogs() {
        if (PermissionUtils.hasPermission(getActivity(), PermissionUtils.PERMISSIONS_STORAGE)) {
            commonsLogSender.send(getActivity(), null);
        } else {
            requestExternalStoragePermissions();
        }
    }

    /**
     * Requests external storage permissions and shows a toast stating that log collection has
     * started
     */
    private void requestExternalStoragePermissions() {
        Dexter.withActivity(getActivity())
            .withPermissions(PermissionUtils.PERMISSIONS_STORAGE)
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    ViewUtil.showLongToast(getActivity(),
                        getResources().getString(R.string.log_collection_started));
                }

                @Override
                public void onPermissionRationaleShouldBeShown(
                    List<PermissionRequest> permissions, PermissionToken token) {

                }
            })
            .onSameThread()
            .check();
    }
}
