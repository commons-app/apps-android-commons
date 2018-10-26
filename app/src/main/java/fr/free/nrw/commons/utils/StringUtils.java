package fr.free.nrw.commons.utils;

import android.os.Build;
import android.text.Html;

public class StringUtils {
    public static String getParsedStringFromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            //noinspection deprecation
            return Html.fromHtml(source).toString();
        }
    }
}
