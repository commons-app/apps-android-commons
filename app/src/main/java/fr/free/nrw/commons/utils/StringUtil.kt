package fr.free.nrw.commons.utils

import android.text.Spanned
import android.text.SpannedString
import androidx.core.text.HtmlCompat

object StringUtil {
    // Marks where a definition list item starts or ends, before the marks become line breaks.
    private const val LIST_BREAK = ""
    private val TERM_START = Regex("<dt\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val TERM_END = Regex("</dt\\s*>", RegexOption.IGNORE_CASE)
    private val LIST_OR_ITEM_TAG = Regex("</?(?:dl|dd)\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val LIST_BREAKS = Regex("\\s*$LIST_BREAK(?:\\s*$LIST_BREAK)*\\s*")
    private val LIST_BREAKS_AT_ENDS = Regex("^$LIST_BREAK|$LIST_BREAK$")

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
        val processedSource =
            source
                .replace("&#8206;", "\u200E")
                .replace("&#8207;", "\u200F")
                .replace("&amp;", "&")
                .let(::breakDefinitionLists)

        return HtmlCompat.fromHtml(processedSource, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Puts each item of a definition list on its own line.
     *
     * Lines starting with ":" or ";" in wikitext become `<dl>` lists with `<dd>` and `<dt>` items,
     * which HtmlCompat ignores, so the items would otherwise run together.
     */
    private fun breakDefinitionLists(source: String): String {
        if (!source.contains("<d", ignoreCase = true)) {
            return source
        }
        return source
            .replace(TERM_START, "$LIST_BREAK<b>")
            .replace(TERM_END, "</b>$LIST_BREAK")
            .replace(LIST_OR_ITEM_TAG, LIST_BREAK)
            .replace(LIST_BREAKS, LIST_BREAK)
            .replace(LIST_BREAKS_AT_ENDS, "")
            .replace(LIST_BREAK, "<br>")
    }
}
