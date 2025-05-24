package fr.free.nrw.commons.upload

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

/**
 * An [InputFilter] class that removes characters blocklisted in Wikimedia titles. The list
 * of blocklisted characters is linked below.
 * @see [](https://commons.wikimedia.org/wiki/MediaWiki:Titleblacklist)wikimedia.org
 */
class UploadMediaDetailInputFilter : InputFilter {
    private val patterns = listOf(
        Pattern.compile("[\\x{00A0}\\x{1680}\\x{180E}\\x{2000}-\\x{200B}\\x{2028}\\x{2029}\\x{202F}\\x{205F}]"),
        Pattern.compile("[\\x{202A}-\\x{202E}]"),
        Pattern.compile("\\p{Cc}"),
        Pattern.compile("\\x{3A}"),  // Added for colon(:)
        Pattern.compile("\\x{FEFF}"),
        Pattern.compile("\\x{00AD}"),
        Pattern.compile("[\\x{E000}-\\x{F8FF}\\x{FFF0}-\\x{FFFF}]"),
        Pattern.compile("[^\\x{0000}-\\x{FFFF}\\p{sc=Han}]")
    )

    /**
     * Checks if the source text contains any blocklisted characters.
     * @param source input text
     * @return contains a blocklisted character
     */
    private fun checkBlocklisted(source: CharSequence): Boolean =
        patterns.any { it.matcher(source).find() }

    /**
     * Removes any blocklisted characters from the input text.
     *
     * @param input The input text to be cleaned.
     * @return A cleaned character sequence with blocklisted patterns removed.
     */
    private fun removeBlocklisted(input: CharSequence): CharSequence {
        var source = input
        patterns.forEach {
            source = it.matcher(source).replaceAll("")
        }

        return source
    }

    /**
     * Filters out any blocklisted characters.
     * @param source {@inheritDoc}
     * @param start {@inheritDoc}
     * @param end {@inheritDoc}
     * @param dest {@inheritDoc}
     * @param dstart {@inheritDoc}
     * @param dend {@inheritDoc}
     * @return {@inheritDoc}
     */
    override fun filter(
        source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int,
        dend: Int
    ): CharSequence? {
        if (checkBlocklisted(source)) {
            if (start == dstart && dest.isNotEmpty()) {
                return dest
            }

            return removeBlocklisted(source)
        }
        return null
    }
}