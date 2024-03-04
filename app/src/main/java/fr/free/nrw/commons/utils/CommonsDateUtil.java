package fr.free.nrw.commons.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Provides util functions for formatting date time
 * Most of our formatting needs are addressed by the data library's DateUtil class
 * Methods should be added here only if DateUtil class doesn't provide for it already
 */
public class CommonsDateUtil {

    /**
     * Gets SimpleDateFormat for short date pattern
     * @return simpledateformat
     */
    public static SimpleDateFormat getIso8601DateFormatShort() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    /**
     * Gets SimpleDateFormat for date pattern returned by Media object
     * @return simpledateformat
     */
    public static SimpleDateFormat getMediaSimpleDateFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    /**
     * Gets the timestamp pattern for a date
     * @return timestamp
     */
        public static SimpleDateFormat getIso8601DateFormatTimestamp() {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.ROOT);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return simpleDateFormat;
    }
}
