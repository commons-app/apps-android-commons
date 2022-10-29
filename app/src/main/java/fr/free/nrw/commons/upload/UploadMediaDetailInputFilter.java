package fr.free.nrw.commons.upload;

import android.text.InputFilter;
import android.text.Spanned;
import java.util.regex.Pattern;

/**
 * An {@link InputFilter} class that removes characters blocklisted in Wikimedia titles. The list
 * of blocklisted characters is linked below.
 * @see <a href="https://commons.wikimedia.org/wiki/MediaWiki:Titleblacklist"></a>wikimedia.org</a>
 */
public class UploadMediaDetailInputFilter implements InputFilter {
    private final Pattern[] patterns;

    /**
     * Initializes the blocklisted patterns.
     */
    public UploadMediaDetailInputFilter() {
        patterns = new Pattern[]{
            Pattern.compile("[\\x{00A0}\\x{1680}\\x{180E}\\x{2000}-\\x{200B}\\x{2028}\\x{2029}\\x{202F}\\x{205F}]"),
            Pattern.compile("[\\x{202A}-\\x{202E}]"),
            Pattern.compile("\\p{Cc}"),
            Pattern.compile("\\x{FEFF}"),
            Pattern.compile("\\x{00AD}"),
            Pattern.compile("[\\x{E000}-\\x{F8FF}\\x{FFF0}-\\x{FFFF}]"),
            Pattern.compile("[^\\x{0000}-\\x{FFFF}\\p{sc=Han}]")
        };
    }

    /**
     * Checks if the source text contains any blocklisted characters.
     * @param source input text
     * @return contains a blocklisted character
     */
    private Boolean checkBlocklisted(final CharSequence source) {
        for (final Pattern pattern: patterns) {
            if (pattern.matcher(source).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes any blocklisted characters from the source text.
     * @param source input text
     * @return a cleaned character sequence
     */
    private CharSequence removeBlocklisted(CharSequence source) {
        for (final Pattern pattern: patterns) {
            source = pattern.matcher(source).replaceAll("");
        }

        return source;
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
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
        int dend) {
        if (checkBlocklisted(source)) {
            if (start == dstart) {
                return dest;
            }

            return removeBlocklisted(source);
        }
        return null;
    }
}