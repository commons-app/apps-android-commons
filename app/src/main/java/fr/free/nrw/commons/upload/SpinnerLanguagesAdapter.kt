package fr.free.nrw.commons.upload

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.LayoutRes
import androidx.core.os.ConfigurationCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.utils.LangCodeUtils
import kotlinx.android.synthetic.main.row_item_languages_spinner.*
import kotlinx.android.synthetic.main.row_item_languages_spinner.view.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.language.AppLanguageLookUpTable
import java.util.*

/**
 * This class handles the display of language spinners and their dropdown views for UploadMediaDetailFragment
 *
 * @property selectedLanguages - controls the enabled state of dropdown views
 *
 * @param context - required by super constructor
 */
class SpinnerLanguagesAdapter constructor(
    context: Context,
    private val selectedLanguages: HashMap<*, String>,
    private var languageNamesList: List<String>,
    private val languageCodesList: List<String>
) : ArrayAdapter<String?>(context, R.layout.row_item_languages_spinner) {

    var language: AppLanguageLookUpTable = AppLanguageLookUpTable(context)
    private val filter = LanguageFilter()
    var selectedLangCode = ""

    override fun isEnabled(position: Int) = languageCodesList[position].let {
        it.isNotEmpty() && !selectedLanguages.containsValue(it) && it != selectedLangCode
    }

    override fun getCount() = languageNamesList.size


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = parent.inflate(R.layout.row_item_languages_spinner)
        val languageCode = languageCodesList[position]
        val languageName = languageNamesList[position]
        rowView.tv_language.let {
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

    fun getIndexOfUserDefaultLocale(context: Context): Int {
        return languageCodesList.indexOf(context.locale.language)
    }

    fun getIndexOfLanguageCode(languageCode: String): Int {
        return languageCodesList.indexOf(languageCode)
    }


    override fun getFilter() = filter

    inner class LanguageFilter() : Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            val tempList: ArrayList<String> = ArrayList<String>()
            if (constraint != null && language.localizedNames != null) {
                val length: Int = language.localizedNames.size
                var i = 0
                while (i < length) {
                    val item: String = language.localizedNames.get(i)
                    val uilang = getIndexOfUserDefaultLocale(context)
                    if(item.contains(constraint, true) || Locale(languageCodesList[i]).getDisplayName(
                            Locale(languageCodesList[uilang])).contains(constraint, true))
                        tempList.add(item)
                    i++
                }
                filterResults.values = tempList
                filterResults.count = tempList.size
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            languageNamesList = results.values as List<String>
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }

        }

    }

}

private fun ViewGroup.inflate(@LayoutRes resId: Int) =
    LayoutInflater.from(context).inflate(resId, this, false)

private val Context.locale: Locale
    get() = ConfigurationCompat.getLocales(resources.configuration)[0]
