package fr.free.nrw.commons.recentlanguages

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.RowItemLanguagesSpinnerBinding
import fr.free.nrw.commons.utils.LangCodeUtils
import org.apache.commons.lang3.StringUtils
import java.util.HashMap

/**
 * Array adapter for recent languages
 */
class RecentLanguagesAdapter constructor(
    context: Context,
    var recentLanguages: List<Language>,
    private val selectedLanguages: HashMap<*, String>
) : ArrayAdapter<String?>(context, R.layout.row_item_languages_spinner) {

    /**
     * Selected language code in UploadMediaDetailAdapter
     * Used for marking selected ones
     */
    var selectedLangCode = ""

    override fun isEnabled(position: Int) = recentLanguages[position].languageCode.let {
        it.isNotEmpty() && !selectedLanguages.containsValue(it) && it != selectedLangCode
    }

    override fun getCount() = recentLanguages.size

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

        val languageCode = recentLanguages[position].languageCode
        val languageName = recentLanguages[position].languageName
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

    /**
     * Provides code of a language from recent languages for a specific position
     */
    fun getLanguageCode(position: Int): String {
        return recentLanguages[position].languageCode
    }

    /**
     * Provides name of a language from recent languages for a specific position
     */
    fun getLanguageName(position: Int): String {
        return recentLanguages[position].languageName
    }
}