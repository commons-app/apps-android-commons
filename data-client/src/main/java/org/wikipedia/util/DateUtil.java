package org.wikipedia.util;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.feed.model.UtcDate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

    public static synchronized String iso8601LocalDateFormat(Date date) {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT, false).format(date);
    }

    public static String getMonthOnlyDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMMM d");
    }

    public static String getMonthOnlyWithoutDayDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMMM");
    }

    public static String getExtraShortDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMM d");
    }

    public static synchronized String getDateStringWithSkeletonPattern(@NonNull Date date, @NonNull String pattern) {
        return getCachedDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault(), false).format(date);
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

    public static String getShortDateString(@NonNull Context context, @NonNull Date date) {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static UtcDate getUtcRequestDateFor(int age) {
        return new UtcDate(age);
    }

    public static Calendar getDefaultDateFor(int age) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.add(Calendar.DATE, -age);
        return calendar;
    }

    public static synchronized Date getHttpLastModifiedDate(@NonNull String dateStr) throws ParseException {
        return getCachedDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH, true).parse(dateStr);
    }

    public static String getReadingListsLastSyncDateString(@NonNull String dateStr) throws ParseException {
        return getDateStringWithSkeletonPattern(iso8601DateParse(dateStr), "d MMM yyyy HH:mm");
    }

    @NonNull public static String yearToStringWithEra(int year) {
        Calendar cal = new GregorianCalendar(year, 1, 1);
        return getDateStringWithSkeletonPattern(cal.getTime(), year < 0 ? "y GG" : "y");
    }

    private DateUtil() {
    }
}
