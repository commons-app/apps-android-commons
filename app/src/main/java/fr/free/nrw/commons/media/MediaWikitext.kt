package fr.free.nrw.commons.media

import androidx.core.text.HtmlCompat
import java.util.Locale

private const val MAX_CAPTION_LENGTH = 250
private const val ELLIPSIS = "..."
private val WHITESPACE_REGEX = Regex("[\\s\\p{Z}\\u0085]+")

/**
 * Formats a Commons file title and its structured caption or HTML description for copying into a
 * wiki page. Prefers the structured data caption in [language] when non-empty, falling back to
 * [description], collapses whitespace, truncates long captions, and escapes wikitext delimiters.
 */
internal fun formatMediaWikitext(
    filename: String,
    description: String?,
    captions: Map<String, String> = emptyMap(),
    language: String = Locale.getDefault().language,
): String {
    val plainCaption = selectNormalizedCaption(description, captions, language)
    val truncated = truncateCaption(plainCaption)
    val escaped = escapeWikitextDelimiters(truncated)
    return if (escaped.isEmpty()) "[[$filename|thumb]]" else "[[$filename|thumb|$escaped]]"
}

private fun selectNormalizedCaption(
    description: String?,
    captions: Map<String, String>,
    language: String,
): String {
    val structuredCaption = captions[language]?.let(::collapseWhitespace).orEmpty()
    return structuredCaption.ifEmpty {
        collapseWhitespace(htmlToPlainText(description.orEmpty()))
    }
}

private fun htmlToPlainText(source: String): String =
    if ('<' in source || '&' in source) {
        HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    } else {
        source
    }

private fun collapseWhitespace(text: String): String =
    // Android represents inline images, including Commons edit icons, with \uFFFC;
    // \p{Z} matches Unicode separators such as NBSP (\u00A0) that JVM \s does not match.
    text
        .replace("\uFFFC", "")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

private fun truncateCaption(caption: String): String {
    if (caption.length <= MAX_CAPTION_LENGTH) {
        return caption
    }
    val maxPrefixLength = MAX_CAPTION_LENGTH - ELLIPSIS.length
    val lastSpace = caption.lastIndexOf(' ', startIndex = maxPrefixLength)
    val cutEnd =
        if (lastSpace > 0) {
            lastSpace
        } else {
            adjustedCutIndex(caption, maxPrefixLength)
        }
    return caption.substring(0, cutEnd) + ELLIPSIS
}

private fun adjustedCutIndex(
    text: String,
    maxIndex: Int,
): Int =
    if (maxIndex > 0 && Character.isHighSurrogate(text[maxIndex - 1])) {
        maxIndex - 1
    } else {
        maxIndex
    }

private fun escapeWikitextDelimiters(caption: String): String =
    caption
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("|", "&#124;")
        .replace("[", "&#91;")
        .replace("]", "&#93;")
        .replace("{", "&#123;")
        .replace("}", "&#125;")
        .replace("=", "&#61;")
