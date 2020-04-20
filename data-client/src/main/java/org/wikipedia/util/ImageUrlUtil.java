package org.wikipedia.util;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageUrlUtil {
    private static Pattern WIDTH_IN_IMAGE_URL_REGEX = Pattern.compile("/(\\d+)px-");

    @NonNull
    public static Uri getUrlForSize(@NonNull Uri uri, int size) {
        return Uri.parse(getUrlForSize(uri.toString(), size));
    }

    @NonNull
    public static String getUrlForSize(@NonNull String original, int size) {
        Matcher matcher = WIDTH_IN_IMAGE_URL_REGEX.matcher(original);
        if (matcher.find() && Integer.parseInt(matcher.group(1)) > size) {
            return matcher.replaceAll("/" + size + "px-");
        }
        return original;
    }

    @NonNull
    public static String getUrlForPreferredSize(@NonNull String original, int size) {
        Matcher matcher = WIDTH_IN_IMAGE_URL_REGEX.matcher(original);
        if (matcher.find() && Integer.parseInt(matcher.group(1)) != size) {
            return matcher.replaceAll("/" + size + "px-");
        }
        return original;
    }

    private ImageUrlUtil() {
    }
}
