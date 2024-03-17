package fr.free.nrw.commons.utils;

import static android.text.format.DateFormat.getBestDateTimePattern;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class DateUtil {
    private static Map<String, SimpleDateFormat> DATE_FORMATS = new HashMap<>();

    // TODO: Switch to DateTimeFormatter when minSdk = 26.

    public static synchronized String iso8601DateFormat(Date date) {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).format(date);
    }

    public static synchronized Date iso8601DateParse(String date) throws ParseException {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).parse(date);
    }

    public static String getMonthOnlyDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMMM d");
    }

    public static String getExtraShortDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMM d");
    }

    public static synchronized String getDateStringWithSkeletonPattern(@NonNull Date date, @NonNull String pattern) {
        return getCachedDateFormat(getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault(), false).format(date);
    }

    private static SimpleDateFormat getCachedDateFormat(String pattern, Locale locale, boolean utc) {
        if (!DATE_FORMATS.containsKey(pattern)) {
            SimpleDateFormat df = new SimpleDateFormat(pattern, locale);
            if (utc) {
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            DATE_FORMATS.put(pattern, df);
        }
        return DATE_FORMATS.get(pattern);
    }

    private DateUtil() {
    }
}
