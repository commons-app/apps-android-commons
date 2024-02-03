package fr.free.nrw.commons.utils;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class StringUtil {

    /**
     * @param source String that may contain HTML tags.
     * @return returned Spanned string that may contain spans parsed from the HTML source.
     */
    @NonNull public static Spanned fromHtml(@Nullable String source) {
        if (source == null) {
            return new SpannedString("");
        }
        if (!source.contains("<") && !source.contains("&")) {
            // If the string doesn't contain any hints of HTML entities, then skip the expensive
            // processing that fromHtml() performs.
            return new SpannedString(source);
        }
        source = source.replaceAll("&#8206;", "\u200E")
                .replaceAll("&#8207;", "\u200F")
                .replaceAll("&amp;", "&");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            return Html.fromHtml(source);
        }
    }

    private StringUtil() {
    }
}
