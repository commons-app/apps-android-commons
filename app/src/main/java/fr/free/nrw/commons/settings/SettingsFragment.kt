package fr.free.nrw.commons.settings

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint
import fr.free.nrw.commons.R
import fr.free.nrw.commons.activity.SingleWebViewActivity
import fr.free.nrw.commons.campaigns.CampaignView
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.logging.CommonsLogSender
import fr.free.nrw.commons.recentlanguages.Language
import fr.free.nrw.commons.recentlanguages.RecentLanguagesAdapter
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.upload.LanguagesAdapter
import fr.free.nrw.commons.utils.DialogUtil
import fr.free.nrw.commons.utils.PermissionUtils
import fr.free.nrw.commons.utils.StringUtil
import fr.free.nrw.commons.utils.ViewUtil
import fr.free.nrw.commons.utils.handleWebUrl
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    @field: Named("default_preferences")
    lateinit var defaultKvStore: JsonKvStore

    @Inject
    lateinit var commonsLogSender: CommonsLogSender

    @Inject
    lateinit var recentLanguagesDao: RecentLanguagesDao

    @Inject
    lateinit var contributionController: ContributionController

    @Inject
    lateinit var locationManager: LocationServiceManager

    private var vanishAccountPreference: Preference? = null
    private var themeListPreference: ListPreference? = null
    private var descriptionLanguageListPreference: Preference? = null
    private var appUiLanguageListPreference: Preference? = null
    private var showDeletionButtonPreference: Preference? = null
    private var keyLanguageListPreference: String? = null
    private var recentLanguagesTextView: TextView? = null
    private var separator: View? = null
    private var languageHistoryListView: ListView? = null

    private lateinit var inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val cameraPickLauncherForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
        contributionController.handleActivityResultWithCallback(
            requireActivity(),
            object: FilePicker.HandleActivityResult {
                override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                    contributionController.onPictureReturnedFromCamera(
                        result,
                        requireActivity(),
                        callbacks
                    )
                }
        })
    }

    /**
     * to be called when the fragment creates preferences
     * @param savedInstanceState the previously saved state
     * @param rootKey the root key for preferences
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Hilt automatically injects dependencies for @AndroidEntryPoint annotated fragments

        // Set the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)

        themeListPreference = findPreference(Prefs.KEY_THEME_VALUE)
        prepareTheme()

        vanishAccountPreference = findPreference(Prefs.VANISHED_ACCOUNT)
        vanishAccountPreference?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.account_vanish_request_confirm_title)
                .setMessage(StringUtil.fromHtml(getString(R.string.account_vanish_request_confirm)))
                .setNegativeButton(R.string.cancel){ dialog,_ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.vanish_account) { dialog, _ ->
                    SingleWebViewActivity.showWebView(
                        context = requireActivity(),
                        url = VANISH_ACCOUNT_URL,
                        successUrl = VANISH_ACCOUNT_SUCCESS_URL
                    )
                    dialog.dismiss()
                }
                .show()
            true
        }

        val multiSelectListPref: MultiSelectListPreference? = findPreference(
            Prefs.MANAGED_EXIF_TAGS
        )
        multiSelectListPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is HashSet<*> && !newValue.contains(getString(R.string.exif_tag_location)))
            {
                defaultKvStore.putBoolean("has_user_manually_removed_location", true)
            }
            true
        }

        val inAppCameraLocationPref: Preference? = findPreference("inAppCameraLocationPref")
        inAppCameraLocationPref?.setOnPreferenceChangeListener { _, newValue ->
            val isInAppCameraLocationTurnedOn = newValue as Boolean
            if (isInAppCameraLocationTurnedOn) {
                createDialogsAndHandleLocationPermissions()
            }
            true
        }

        inAppCameraLocationPermissionLauncher = registerForActivityResult(
            RequestMultiplePermissions()
        ) { result ->
            var areAllGranted = true
            for (b in result.values) {
                areAllGranted = areAllGranted && b
            }
            if (
                !areAllGranted
                    &&
                shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)
            ) {
                contributionController.handleShowRationaleFlowCameraLocation(
                    requireActivity(),
                    inAppCameraLocationPermissionLauncher,
                    cameraPickLauncherForResult
                )
            }
        }

        // Gets current language code from shared preferences
        var languageCode: String?

        appUiLanguageListPreference = findPreference("appUiDefaultLanguagePref")
        appUiLanguageListPreference?.let { appUiLanguageListPreference ->
            keyLanguageListPreference = appUiLanguageListPreference.key
            languageCode = getCurrentLanguageCode(keyLanguageListPreference!!)

            languageCode?.let { code ->
                if (code.isEmpty()) {
                    // If current language code is empty, means none selected by user yet so use
                    // phone locale
                    appUiLanguageListPreference.summary = Locale.getDefault().displayLanguage
                } else {
                    // If any language is selected by user previously, use it
                    val defLocale = createLocale(code)
                    appUiLanguageListPreference.summary = defLocale.getDisplayLanguage(defLocale)
                }
            }

            appUiLanguageListPreference.setOnPreferenceClickListener {
                prepareAppLanguages(keyLanguageListPreference!!)
                true
            }
        }

        descriptionLanguageListPreference = findPreference("descriptionDefaultLanguagePref")
        descriptionLanguageListPreference?.let { descriptionLanguageListPreference ->
            languageCode = getCurrentLanguageCode(descriptionLanguageListPreference.key)

            languageCode?.let { code ->
                if (code.isEmpty()) {
                    // If current language code is empty, means none selected by user yet so use
                    // phone locale
                    descriptionLanguageListPreference.summary = Locale.getDefault().displayLanguage
                } else {
                    // If any language is selected by user previously, use it
                    val defLocale = createLocale(code)
                    descriptionLanguageListPreference.summary = defLocale.getDisplayLanguage(
                        defLocale
                    )
                }
            }

            descriptionLanguageListPreference.setOnPreferenceClickListener {
                prepareAppLanguages(it.key)
                true
            }
        }

        showDeletionButtonPreference = findPreference("displayDeletionButton")
        showDeletionButtonPreference?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            // Save preference when user toggles the button
            defaultKvStore.putBoolean("displayDeletionButton", isEnabled)
            true
        }

        val betaTesterPreference: Preference? = findPreference("becomeBetaTester")
        betaTesterPreference?.setOnPreferenceClickListener {
            handleWebUrl(
                requireActivity(),
                Uri.parse(getString(R.string.beta_opt_in_link))
            )
            true
        }

        val sendLogsPreference: Preference? = findPreference("sendLogFile")
        sendLogsPreference?.setOnPreferenceClickListener {
            checkPermissionsAndSendLogs()
            true
        }

        val documentBasedPickerPreference: Preference? = findPreference(
            "openDocumentPhotoPickerPref"
        )
        documentBasedPickerPreference?.setOnPreferenceChangeListener { _, newValue ->
            val isGetContentPickerTurnedOn = newValue as Boolean
            if (!isGetContentPickerTurnedOn) {
                showLocationLossWarning()
            }
            true
        }

        // Disable some settings when not logged in.
        if (defaultKvStore.getBoolean("login_skipped", false)) {
            findPreference<Preference>("useExternalStorage")?.isEnabled = false
            findPreference<Preference>("useAuthorName")?.isEnabled = false
            findPreference<Preference>("displayNearbyCardView")?.isEnabled = false
            findPreference<Preference>("descriptionDefaultLanguagePref")?.isEnabled = false
            findPreference<Preference>("displayLocationPermissionForCardView")?.isEnabled = false
            findPreference<Preference>(CampaignView.CAMPAIGNS_DEFAULT_PREFERENCE)?.isEnabled = false
            findPreference<Preference>("managed_exif_tags")?.isEnabled = false
            findPreference<Preference>("openDocumentPhotoPickerPref")?.isEnabled = false
            findPreference<Preference>("inAppCameraLocationPref")?.isEnabled = false
            findPreference<Preference>("vanishAccount")?.isEnabled = false
        }
    }

    /**
     * Asks users to provide location access.
     */
    private fun createDialogsAndHandleLocationPermissions() {
        inAppCameraLocationPermissionLauncher.launch(arrayOf(permission.ACCESS_FINE_LOCATION))
    }

    /**
     * On some devices, the new Photo Picker with GET_CONTENT takeover
     * redacts location tags from EXIF metadata
     *
     * Show warning to the user when ACTION_GET_CONTENT intent is enabled
     */
    private fun showLocationLossWarning() {
        DialogUtil.showAlertDialog(
            requireActivity(),
            null,
            getString(R.string.location_loss_warning),
            getString(R.string.ok),
            getString(R.string.read_help_link),
            { },
            { handleWebUrl(requireContext(), Uri.parse(GET_CONTENT_PICKER_HELP_URL)) },
            null
        )
    }

    // Remove the space for icons in the settings menu.
    // This uses an internal API that shouldn't be used in app code,
    // but it appears to be the most robust way to do this at the moment,
    // disable the warning.
    @SuppressLint("RestrictedApi")
    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): Adapter<PreferenceViewHolder>
    {
        return object : PreferenceGroupAdapter(preferenceScreen) {
            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                val iconFrame: View? = holder.itemView.findViewById(R.id.icon_frame)
                iconFrame?.visibility = View.GONE
            }
        }
    }

    /**
     * Sets the theme pref
     */
    private fun prepareTheme() {
        themeListPreference?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }
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
    private fun prepareAppLanguages(keyListPreference: String) {
        // Gets current language code from shared preferences
        val languageCode = getCurrentLanguageCode(keyListPreference)
        val recentLanguages = recentLanguagesDao.getRecentLanguages()
        val selectedLanguages = hashMapOf<Int, String>()

        if (keyListPreference == "appUiDefaultLanguagePref") {
            if (languageCode.isNullOrEmpty()) {
                selectedLanguages[0] = Locale.getDefault().language
            } else {
                selectedLanguages[0] = languageCode
            }
        } else if (keyListPreference == "descriptionDefaultLanguagePref") {
            if (languageCode.isNullOrEmpty()) {
                selectedLanguages[0] = Locale.getDefault().language
            } else {
                selectedLanguages[0] = languageCode
            }
        }

        val languagesAdapter = LanguagesAdapter(requireActivity(), selectedLanguages)

        val dialog = Dialog(requireActivity())
        dialog.setContentView(R.layout.dialog_select_language)
        dialog.setCancelable(false)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            (resources.displayMetrics.heightPixels * 0.90).toInt()
        )
        dialog.show()

        val editText: EditText = dialog.findViewById(R.id.search_language)
        val listView: ListView = dialog.findViewById(R.id.language_list)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        languageHistoryListView = dialog.findViewById(R.id.language_history_list)
        recentLanguagesTextView = dialog.findViewById(R.id.recent_searches)
        separator = dialog.findViewById(R.id.separator)

        setUpRecentLanguagesSection(recentLanguages, selectedLanguages)

        listView.adapter = languagesAdapter

        cancelButton.setOnClickListener { dialog.dismiss() }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
                hideRecentLanguagesSection()
            }

            override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
                languagesAdapter.filter.filter(charSequence)
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        languageHistoryListView?.setOnItemClickListener { adapterView, _, position, _ ->
            onRecentLanguageClicked(keyListPreference, dialog, adapterView, position)
        }

        listView.setOnItemClickListener { adapterView, _, position, _ ->
            val lCode = (adapterView.adapter as LanguagesAdapter).getLanguageCode(position)
            val languageName = (adapterView.adapter as LanguagesAdapter).getLanguageName(position)
            val isExists = recentLanguagesDao.findRecentLanguage(lCode)
            if (isExists) {
                recentLanguagesDao.deleteRecentLanguage(lCode)
            }
            recentLanguagesDao.addRecentLanguage(Language(languageName, lCode))
            saveLanguageValue(lCode, keyListPreference)
            val defLocale = createLocale(lCode)
            if (keyListPreference == "appUiDefaultLanguagePref") {
                appUiLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
                setLocale(requireActivity(), lCode)
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                requireActivity().finish()
                startActivity(intent)
            }
                else {
                descriptionLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
            }
            dialog.dismiss()
        }

        dialog.setOnDismissListener { languagesAdapter.filter.filter("") }
    }

    /**
     * Set up recent languages section
     *
     * @param recentLanguages recently used languages
     * @param selectedLanguages selected languages
     */
    private fun setUpRecentLanguagesSection(
        recentLanguages: List<Language>,
        selectedLanguages: HashMap<Int, String>
    ) {
        if (recentLanguages.isEmpty()) {
            languageHistoryListView?.visibility = View.GONE
            recentLanguagesTextView?.visibility = View.GONE
            separator?.visibility = View.GONE
        } else {
            if (recentLanguages.size > 5) {
                for (i in recentLanguages.size - 1 downTo 5) {
                    recentLanguagesDao.deleteRecentLanguage(recentLanguages[i].languageCode)
                }
            }
            languageHistoryListView?.visibility = View.VISIBLE
            recentLanguagesTextView?.visibility = View.VISIBLE
            separator?.visibility = View.VISIBLE
            val recentLanguagesAdapter = RecentLanguagesAdapter(
                requireActivity(),
                recentLanguagesDao.getRecentLanguages(),
                selectedLanguages
            )
            languageHistoryListView?.adapter = recentLanguagesAdapter
        }
    }

    /**
     * Handles click event for recent language section
     */
    private fun onRecentLanguageClicked(
        keyListPreference: String,
        dialog: Dialog,
        adapterView: AdapterView<*>,
        position: Int
    ) {
        val recentLanguageCode = (adapterView.adapter as RecentLanguagesAdapter).getLanguageCode(position)
        val recentLanguageName = (adapterView.adapter as RecentLanguagesAdapter).getLanguageName(position)
        val isExists = recentLanguagesDao.findRecentLanguage(recentLanguageCode)
        if (isExists) {
            recentLanguagesDao.deleteRecentLanguage(recentLanguageCode)
        }
        recentLanguagesDao.addRecentLanguage(Language(recentLanguageName, recentLanguageCode))
        saveLanguageValue(recentLanguageCode, keyListPreference)
        val defLocale = createLocale(recentLanguageCode)
        if (keyListPreference == "appUiDefaultLanguagePref") {
            appUiLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
            setLocale(requireActivity(), recentLanguageCode)
            requireActivity().recreate()
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        } else {
            descriptionLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
        }
        dialog.dismiss()
    }

    /**
     * Remove the section of recent languages
     */
    private fun hideRecentLanguagesSection() {
        languageHistoryListView?.visibility = View.GONE
        recentLanguagesTextView?.visibility = View.GONE
        separator?.visibility = View.GONE
    }

    /**
     * Changing the default app language with selected one and save it to SharedPreferences
     */
    fun setLocale(activity: Activity, userSelectedValue: String) {
        var selectedLanguage = userSelectedValue
        if (selectedLanguage == "") {
            selectedLanguage = Locale.getDefault().language
        }
        val locale = createLocale(selectedLanguage)
        Locale.setDefault(locale)
        val configuration = Configuration()
        configuration.locale = locale
        activity.baseContext.resources.updateConfiguration(configuration, activity.baseContext.resources.displayMetrics)

        val editor = activity.getSharedPreferences("Settings", MODE_PRIVATE).edit()
        editor.putString("language", selectedLanguage)
        editor.apply()
    }

    @Suppress("LongLine")
    companion object {
        const val GET_CONTENT_PICKER_HELP_URL = "https://commons-app.github.io/docs.html#get-content"
        private const val VANISH_ACCOUNT_URL = "https://meta.m.wikimedia.org/wiki/Special:Contact/accountvanishapps"
        private const val VANISH_ACCOUNT_SUCCESS_URL = "https://meta.m.wikimedia.org/wiki/Special:GlobalVanishRequest/vanished"
        /**
         * Create Locale based on different types of language codes
         * @param languageCode
         * @return Locale and throws error for invalid language codes
         */
        fun createLocale(languageCode: String): Locale {
            val parts = languageCode.split("-")
            return when (parts.size) {
                1 -> Locale(parts[0])
                2 -> Locale(parts[0], parts[1])
                3 -> Locale(parts[0], parts[1], parts[2])
                else -> throw IllegalArgumentException("Invalid language code: $languageCode")
            }
        }
    }

    /**
     * Save userSelected language in List Preference
     * @param userSelectedValue
     * @param preferenceKey
     */
    private fun saveLanguageValue(userSelectedValue: String, preferenceKey: String) {
        when (preferenceKey) {
            "appUiDefaultLanguagePref" -> defaultKvStore.putString(Prefs.APP_UI_LANGUAGE, userSelectedValue)
            "descriptionDefaultLanguagePref" -> defaultKvStore.putString(Prefs.DESCRIPTION_LANGUAGE, userSelectedValue)
        }
    }

    /**
     * Gets current language code from shared preferences
     * @param preferenceKey
     * @return
     */
    private fun getCurrentLanguageCode(preferenceKey: String): String? {
        return when (preferenceKey) {
            "appUiDefaultLanguagePref" -> defaultKvStore.getString(
                Prefs.APP_UI_LANGUAGE, ""
            )
            "descriptionDefaultLanguagePref" -> defaultKvStore.getString(
                Prefs.DESCRIPTION_LANGUAGE, ""
            )
            else -> null
        }
    }

    /**
     * First checks for external storage permissions and then sends logs via email
     */
    private fun checkPermissionsAndSendLogs() {
        if (
            PermissionUtils.hasPermission(
                requireActivity(),
                PermissionUtils.PERMISSIONS_STORAGE
            )
        ) {
            commonsLogSender.sendWithNullable(requireActivity(), null)
        } else {
            requestExternalStoragePermissions()
        }
    }

    /**
     * Requests external storage permissions and shows a toast stating that log collection has
     * started
     */
    private fun requestExternalStoragePermissions() {
        Dexter.withActivity(requireActivity())
            .withPermissions(*PermissionUtils.PERMISSIONS_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    ViewUtil.showLongToast(requireActivity(), getString(R.string.log_collection_started))
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>, token: PermissionToken
                ) {
                    // No action needed
                }
            })
            .onSameThread()
            .check()
    }

}
