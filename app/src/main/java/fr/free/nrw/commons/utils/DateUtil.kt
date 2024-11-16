package fr.free.nrw.commons.utils

import android.text.format.DateFormat.getBestDateTimePattern
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.TimeZone

/**
 * Utility class for date formatting and parsing.
 * TODO: Switch to DateTimeFormatter when minSdk = 26.
 */
object DateUtil {

    private val DATE_FORMATS: MutableMap<String, SimpleDateFormat> = HashMap()

    @JvmStatic
    @Synchronized
    fun iso8601DateFormat(date: Date): String {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).format(date)
    }

    @JvmStatic
    @Synchronized
    @Throws(ParseException::class)
    fun iso8601DateParse(date: String): Date {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).parse(date)
    }

    @JvmStatic
    fun getMonthOnlyDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMMM d")
    }

    @JvmStatic
    fun getExtraShortDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMM d")
    }

    @JvmStatic
    @Synchronized
    fun getDateStringWithSkeletonPattern(date: Date, pattern: String): String {
        return getCachedDateFormat(
            getBestDateTimePattern(Locale.getDefault(), pattern),
            Locale.getDefault(), false
        ).format(date)
    }

    @JvmStatic
    private fun getCachedDateFormat(pattern: String, locale: Locale, utc: Boolean): SimpleDateFormat {
        if (!DATE_FORMATS.containsKey(pattern)) {
            val df = SimpleDateFormat(pattern, locale)
            if (utc) {
                df.timeZone = TimeZone.getTimeZone("UTC")
            }
            DATE_FORMATS[pattern] = df
        }
        return DATE_FORMATS[pattern]!!
    }
}
