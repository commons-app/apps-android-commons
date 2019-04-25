package fr.free.nrw.commons.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class CommonsDateUtil {

    public static SimpleDateFormat getIso8601DateFormatShort() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }
}
