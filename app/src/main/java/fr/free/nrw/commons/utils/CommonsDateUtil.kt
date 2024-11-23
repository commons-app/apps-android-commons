package fr.free.nrw.commons.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Provides util functions for formatting date time.
 * Most of our formatting needs are addressed by the data library's DateUtil class.
 * Methods should be added here only if DateUtil class doesn't provide for it already.
 */
object CommonsDateUtil {

    /**
     * Gets SimpleDateFormat for short date pattern.
     * @return simpleDateFormat
     */
    @JvmStatic
    fun getIso8601DateFormatShort(): SimpleDateFormat {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat
    }

    /**
     * Gets SimpleDateFormat for date pattern returned by Media object.
     * @return simpleDateFormat
     */
    @JvmStatic
    fun getMediaSimpleDateFormat(): SimpleDateFormat {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat
    }

    /**
     * Gets the timestamp pattern for a date.
     * @return timestamp
     */
    @JvmStatic
    fun getIso8601DateFormatTimestamp(): SimpleDateFormat {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat
    }
}
