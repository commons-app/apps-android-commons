package fr.free.nrw.commons.upload

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.core.os.ConfigurationCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.RowItemLanguagesSpinnerBinding
import fr.free.nrw.commons.utils.LangCodeUtils
import org.apache.commons.lang3.StringUtils
import org.wikipedia.language.AppLanguageLookUpTable
import java.util.*

/**
 * This class handles the display of language dialog and their views for UploadMediaDetailFragment
 *
 * @property selectedLanguages - controls the enabled state of item views
 *
 * @param context - required by super constructor
 */
class LanguagesAdapter constructor(
    context: Context,
    private val selectedLanguages: HashMap<*, String>
) : ArrayAdapter<String?>(context, R.layout.row_item_languages_spinner) {
    companion object {
        const val DEFAULT_INDEX = 0
    }

    private var languageNamesList: List<String>
    private var languageCodesList: List<String>

    var language: AppLanguageLookUpTable = AppLanguageLookUpTable(context)
    init {
        languageNamesList = language.localizedNames
        languageCodesList = language.codes
    }

    private val filter = LanguageFilter()
    var selectedLangCode = ""

    override fun isEnabled(position: Int) = languageCodesList[position].let {
        it.isNotEmpty() && !selectedLanguages.containsValue(it) && it != selectedLangCode
    }

    override fun getCount() = languageNamesList.size


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: RowItemLanguagesSpinnerBinding
        var rowView = convertView

        if (rowView == null) {
            val layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = RowItemLanguagesSpinnerBinding.inflate(layoutInflater, parent, false)
            rowView = binding.root
        } else {
            binding = RowItemLanguagesSpinnerBinding.bind(rowView)
        }
        val languageCode = languageCodesList[position]
        val languageName = languageNamesList[position]

        binding.tvLanguage.let {
            it.isEnabled = isEnabled(position)
            if (languageCode.isEmpty()) {
                it.text = StringUtils.capitalize(languageName)
                it.textAlignment = View.TEXT_ALIGNMENT_CENTER
            } else {
                it.text =
                    "${StringUtils.capitalize(languageName)}" +
                            " [${LangCodeUtils.fixLanguageCode(languageCode)}]"
            }
        }
        return rowView
    }

    fun getLanguageCode(position: Int): String {
        return languageCodesList[position]
    }

    /**
     * Provides name of a language from languages for a specific position
     */
    fun getLanguageName(position: Int): String {
        return languageNamesList[position]
    }

    fun getIndexOfUserDefaultLocale(context: Context): Int {

        val userLanguageCode = context.locale?.language ?: return DEFAULT_INDEX
        return language.codes.indexOf(userLanguageCode).takeIf { it >= 0 } ?: DEFAULT_INDEX
    }

    fun getIndexOfLanguageCode(languageCode: String): Int {
        return languageCodesList.indexOf(languageCode)
    }


    override fun getFilter() = filter

    inner class LanguageFilter : Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            val temp: LinkedHashMap<String, String> = LinkedHashMap()
            if (constraint != null && language.localizedNames != null) {
                val length: Int = language.localizedNames.size
                var i = 0
                while (i < length) {
                    val key: String = language.codes[i]
                    val value: String = language.localizedNames[i]
                    val defaultlanguagecode = getIndexOfUserDefaultLocale(context)
                    if(value.contains(constraint, true) || Locale(key).getDisplayName(
                            Locale(language.codes[defaultlanguagecode])).contains(constraint, true))
                        temp[key] = value
                    i++
                }
                filterResults.values = temp
                filterResults.count = temp.size
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            if (results.count > 0) {
                languageCodesList =
                    ArrayList((results.values as LinkedHashMap<String, String>).keys)
                languageNamesList =
                    ArrayList((results.values as LinkedHashMap<String, String>).values)
                notifyDataSetChanged()
            } else {
                languageCodesList = ArrayList()
                languageNamesList = ArrayList()
                notifyDataSetChanged()
            }

        }

    }

}

private val Context.locale: Locale?
    get() = ConfigurationCompat.getLocales(resources.configuration)[0]
