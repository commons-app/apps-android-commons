package fr.free.nrw.commons.media

import androidx.core.text.HtmlCompat

/** Formats a Commons file title and its HTML description for copying into a wiki page. */
internal fun formatMediaWikitext(
    filename: String,
    description: String?,
): String {
    val source = description.orEmpty()
    val text =
        if ('<' in source || '&' in source) {
            HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        } else {
            source
        }
    // Android represents inline images, including Commons edit icons, with this character.
    val caption =
        text
            .replace("\uFFFC", "")
            .trim()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("|", "&#124;")
            .replace("[", "&#91;")
            .replace("]", "&#93;")
            .replace("{", "&#123;")
            .replace("}", "&#125;")
            .replace("=", "&#61;")
    return if (caption.isEmpty()) "[[$filename|thumb]]" else "[[$filename|thumb|$caption]]"
}
