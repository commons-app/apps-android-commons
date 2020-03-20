package fr.free.nrw.commons.upload

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.core.os.ConfigurationCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.utils.BiMap
import fr.free.nrw.commons.utils.LangCodeUtils
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.row_item_languages_spinner.*
import org.apache.commons.lang3.StringUtils
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
    private val selectedLanguages: BiMap<*, String>
) : ArrayAdapter<Any?>(context, -1) {

    private val languageNamesList: List<String>
    private val languageCodesList: List<String>

    init {
        val sortedLanguages = Locale.getAvailableLocales()
            .map(::Language)
            .sortedBy { it.locale.displayName }
        languageNamesList = sortedLanguages.map { it.locale.displayName }
        languageCodesList = sortedLanguages.map { it.locale.language }
    }

    var selectedLangCode = ""

    override fun isEnabled(position: Int) = languageCodesList[position].let {
        it.isNotEmpty() && !selectedLanguages.containsKey(it) && it != selectedLangCode
    }

    override fun getCount() = languageNamesList.size

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
        (convertView ?: parent.inflate(R.layout.row_item_languages_spinner).also {
            it.tag = DropDownViewHolder(it)
        }).apply {
            (tag as DropDownViewHolder).init(
                languageCodesList[position],
                languageNamesList[position],
                isEnabled(position)
            )
        }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
        (convertView ?: parent.inflate(R.layout.row_item_languages_spinner).also {
            it.tag = SpinnerViewHolder(it)
        }).apply { (tag as SpinnerViewHolder).init(languageCodesList[position]) }

    class SpinnerViewHolder(override val containerView: View) : LayoutContainer {
        fun init(languageCode: String) {
            LangCodeUtils.fixLanguageCode(languageCode).let {
                tv_language.text = if (it.length > 2) it.take(2) else it
            }
        }
    }

    class DropDownViewHolder(override val containerView: View) : LayoutContainer {
        fun init(languageCode: String, languageName: String, enabled: Boolean) {
            tv_language.isEnabled = enabled
            if (languageCode.isEmpty()) {
                tv_language.text = StringUtils.capitalize(languageName)
                tv_language.textAlignment = View.TEXT_ALIGNMENT_CENTER
            } else {
                tv_language.text =
                    "${StringUtils.capitalize(languageName)}" +
                            " [${LangCodeUtils.fixLanguageCode(languageCode)}]"
            }
        }
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
}

private fun ViewGroup.inflate(@LayoutRes resId: Int) =
    LayoutInflater.from(context).inflate(resId, this, false)

private val Context.locale: Locale
    get() = ConfigurationCompat.getLocales(resources.configuration)[0]
