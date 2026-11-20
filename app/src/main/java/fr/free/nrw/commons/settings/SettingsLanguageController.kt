package fr.free.nrw.commons.settings

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.preference.Preference
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.recentlanguages.Language
import fr.free.nrw.commons.recentlanguages.RecentLanguagesAdapter
import fr.free.nrw.commons.upload.LanguagesAdapter
import java.util.Locale

/**
 * Controller that handles language preferences, language selection dialogs,
 * and locale configuration for SettingsFragment.
 */
class SettingsLanguageController(
    private val fragment: SettingsFragment,
) {
    private var appUiLanguageListPreference: Preference? = null
    private var descriptionLanguageListPreference: Preference? = null
    private var keyLanguageListPreference: String? = null
    private var recentLanguagesTextView: TextView? = null
    private var separator: View? = null
    private var languageHistoryListView: ListView? = null

    /**
     * Initializes language preferences and sets their initial summary and click listeners.
     */
    fun setupLanguagePreferences() {
        var languageCode: String?

        appUiLanguageListPreference = fragment.findPreference("appUiDefaultLanguagePref")
        appUiLanguageListPreference?.let { pref ->
            keyLanguageListPreference = pref.key
            languageCode = getCurrentLanguageCode(keyLanguageListPreference!!)

            languageCode?.let { code ->
                if (code.isEmpty()) {
                    pref.summary = Locale.getDefault().displayLanguage
                } else {
                    val defLocale = createLocale(code)
                    pref.summary = defLocale.getDisplayLanguage(defLocale)
                }
            }

            pref.setOnPreferenceClickListener {
                prepareAppLanguages(keyLanguageListPreference!!)
                true
            }
        }

        descriptionLanguageListPreference = fragment.findPreference("descriptionDefaultLanguagePref")
        descriptionLanguageListPreference?.let { pref ->
            languageCode = getCurrentLanguageCode(pref.key)

            languageCode?.let { code ->
                if (code.isEmpty()) {
                    pref.summary = Locale.getDefault().displayLanguage
                } else {
                    val defLocale = createLocale(code)
                    pref.summary = defLocale.getDisplayLanguage(defLocale)
                }
            }

            pref.setOnPreferenceClickListener {
                prepareAppLanguages(it.key)
                true
            }
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
    fun prepareAppLanguages(keyListPreference: String) {
        val languageCode = getCurrentLanguageCode(keyListPreference)
        val recentLanguages = fragment.recentLanguagesDao.getRecentLanguages()
        val selectedLanguages = hashMapOf<Int, String>()

        if (keyListPreference == "appUiDefaultLanguagePref" ||
            keyListPreference == "descriptionDefaultLanguagePref"
        ) {
            if (languageCode.isNullOrEmpty()) {
                selectedLanguages[0] = Locale.getDefault().language
            } else {
                selectedLanguages[0] = languageCode
            }
        }

        val activity = fragment.requireActivity()
        val languagesAdapter = LanguagesAdapter(activity, selectedLanguages)

        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_select_language)
        dialog.setCancelable(false)
        dialog.window?.setLayout(
            (fragment.resources.displayMetrics.widthPixels * DIALOG_SIZE_RATIO).toInt(),
            (fragment.resources.displayMetrics.heightPixels * DIALOG_SIZE_RATIO).toInt(),
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

        editText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                    hideRecentLanguagesSection()
                }

                override fun onTextChanged(
                    charSequence: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    languagesAdapter.filter.filter(charSequence)
                }

                override fun afterTextChanged(editable: Editable?) = Unit
            },
        )

        languageHistoryListView?.setOnItemClickListener { adapterView, _, position, _ ->
            onRecentLanguageClicked(keyListPreference, dialog, adapterView, position)
        }

        listView.setOnItemClickListener { adapterView, _, position, _ ->
            onLanguageSelected(keyListPreference, dialog, adapterView, position)
        }

        dialog.setOnDismissListener { languagesAdapter.filter.filter("") }
    }

    /**
     * Handles selection of a language from the full language list.
     *
     * @param keyListPreference the preference key being updated
     * @param dialog the active language selection dialog
     * @param adapterView the adapter view containing the clicked item
     * @param position the index of the clicked language
     */
    private fun onLanguageSelected(
        keyListPreference: String,
        dialog: Dialog,
        adapterView: AdapterView<*>,
        position: Int,
    ) {
        val lCode = (adapterView.adapter as LanguagesAdapter).getLanguageCode(position)
        val languageName = (adapterView.adapter as LanguagesAdapter).getLanguageName(position)
        val isExists = fragment.recentLanguagesDao.findRecentLanguage(lCode)
        if (isExists) {
            fragment.recentLanguagesDao.deleteRecentLanguage(lCode)
        }
        fragment.recentLanguagesDao.addRecentLanguage(Language(languageName, lCode))
        saveLanguageValue(lCode, keyListPreference)
        val defLocale = createLocale(lCode)
        val activity = fragment.requireActivity()
        if (keyListPreference == "appUiDefaultLanguagePref") {
            appUiLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
            setLocale(activity, lCode)
            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            activity.finish()
            fragment.startActivity(intent)
        } else {
            descriptionLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
        }
        dialog.dismiss()
    }

    /**
     * Set up recent languages section
     *
     * @param recentLanguages recently used languages
     * @param selectedLanguages selected languages
     */
    fun setUpRecentLanguagesSection(
        recentLanguages: List<Language>,
        selectedLanguages: HashMap<Int, String>,
    ) {
        if (recentLanguages.isEmpty()) {
            languageHistoryListView?.visibility = View.GONE
            recentLanguagesTextView?.visibility = View.GONE
            separator?.visibility = View.GONE
        } else {
            if (recentLanguages.size > MAX_RECENT_LANGUAGES) {
                for (i in recentLanguages.size - 1 downTo MAX_RECENT_LANGUAGES) {
                    fragment.recentLanguagesDao.deleteRecentLanguage(recentLanguages[i].languageCode)
                }
            }
            languageHistoryListView?.visibility = View.VISIBLE
            recentLanguagesTextView?.visibility = View.VISIBLE
            separator?.visibility = View.VISIBLE
            val recentLanguagesAdapter =
                RecentLanguagesAdapter(
                    fragment.requireActivity(),
                    fragment.recentLanguagesDao.getRecentLanguages(),
                    selectedLanguages,
                )
            languageHistoryListView?.adapter = recentLanguagesAdapter
        }
    }

    /**
     * Handles click event for recent language section
     */
    fun onRecentLanguageClicked(
        keyListPreference: String,
        dialog: Dialog,
        adapterView: AdapterView<*>,
        position: Int,
    ) {
        val recentLanguageCode = (adapterView.adapter as RecentLanguagesAdapter).getLanguageCode(position)
        val recentLanguageName = (adapterView.adapter as RecentLanguagesAdapter).getLanguageName(position)
        val isExists = fragment.recentLanguagesDao.findRecentLanguage(recentLanguageCode)
        if (isExists) {
            fragment.recentLanguagesDao.deleteRecentLanguage(recentLanguageCode)
        }
        fragment.recentLanguagesDao.addRecentLanguage(Language(recentLanguageName, recentLanguageCode))
        saveLanguageValue(recentLanguageCode, keyListPreference)
        val defLocale = createLocale(recentLanguageCode)
        val activity = fragment.requireActivity()
        if (keyListPreference == "appUiDefaultLanguagePref") {
            appUiLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
            setLocale(activity, recentLanguageCode)
            activity.recreate()
            val intent = Intent(activity, MainActivity::class.java)
            fragment.startActivity(intent)
        } else {
            descriptionLanguageListPreference?.summary = defLocale.getDisplayLanguage(defLocale)
        }
        dialog.dismiss()
    }

    /**
     * Remove the section of recent languages
     */
    fun hideRecentLanguagesSection() {
        languageHistoryListView?.visibility = View.GONE
        recentLanguagesTextView?.visibility = View.GONE
        separator?.visibility = View.GONE
    }

    /**
     * Save userSelected language in List Preference
     * @param userSelectedValue
     * @param preferenceKey
     */
    fun saveLanguageValue(
        userSelectedValue: String,
        preferenceKey: String,
    ) {
        when (preferenceKey) {
            "appUiDefaultLanguagePref" ->
                fragment.defaultKvStore.putString(Prefs.APP_UI_LANGUAGE, userSelectedValue)
            "descriptionDefaultLanguagePref" ->
                fragment.defaultKvStore.putString(Prefs.DESCRIPTION_LANGUAGE, userSelectedValue)
        }
    }

    /**
     * Gets current language code from shared preferences
     * @param preferenceKey
     * @return
     */
    fun getCurrentLanguageCode(preferenceKey: String): String? =
        when (preferenceKey) {
            "appUiDefaultLanguagePref" ->
                fragment.defaultKvStore.getString(
                    Prefs.APP_UI_LANGUAGE,
                    "",
                )
            "descriptionDefaultLanguagePref" ->
                fragment.defaultKvStore.getString(
                    Prefs.DESCRIPTION_LANGUAGE,
                    "",
                )
            else -> null
        }

    companion object {
        private const val DIALOG_SIZE_RATIO = 0.90
        private const val MAX_RECENT_LANGUAGES = 5
        private const val LOCALE_PARTS_WITH_VARIANT = 3

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
                LOCALE_PARTS_WITH_VARIANT -> Locale(parts[0], parts[1], parts[2])
                else -> throw IllegalArgumentException("Invalid language code: $languageCode")
            }
        }

        /**
         * Changing the default app language with selected one and save it to SharedPreferences
         */
        fun setLocale(
            activity: Activity,
            userSelectedValue: String,
        ) {
            var selectedLanguage = userSelectedValue
            if (selectedLanguage == "") {
                selectedLanguage = Locale.getDefault().language
            }
            val locale = createLocale(selectedLanguage)
            Locale.setDefault(locale)
            val configuration = Configuration()
            configuration.locale = locale
            activity.baseContext.resources.updateConfiguration(
                configuration,
                activity.baseContext.resources.displayMetrics,
            )

            val editor = activity.getSharedPreferences("Settings", Context.MODE_PRIVATE).edit()
            editor.putString("language", selectedLanguage)
            editor.apply()
        }
    }
}
