package fr.free.nrw.commons.utils

import android.text.Spanned
import android.text.SpannedString
import androidx.core.text.HtmlCompat

object StringUtil {

    /**
     * @param source String that may contain HTML tags.
     * @return returned Spanned string that may contain spans parsed from the HTML source.
     */
    @JvmStatic
    fun fromHtml(source: String?): Spanned {
        if (source == null) {
            return SpannedString("")
        }
        if (!source.contains("<") && !source.contains("&")) {
            // If the string doesn't contain any hints of HTML entities, then skip the expensive
            // processing that fromHtml() performs.
            return SpannedString(source)
        }
        val processedSource = source
            .replace("&#8206;", "\u200E")
            .replace("&#8207;", "\u200F")
            .replace("&amp;", "&")
            .replace(Regex("(?is)<img\\b[^>]*>"), "")

        return HtmlCompat.fromHtml(processedSource, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

}
