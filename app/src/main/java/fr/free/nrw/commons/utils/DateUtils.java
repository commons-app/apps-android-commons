package fr.free.nrw.commons.utils;

import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat isoFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);

    public static String getTimeAgo(Date currDate, Date itemDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(currDate);
        int yearNow = c.get(Calendar.YEAR);
        int monthNow = c.get(Calendar.MONTH);
        int dayNow = c.get(Calendar.DAY_OF_MONTH);
        int hourNow = c.get(Calendar.HOUR_OF_DAY);
        int minuteNow = c.get(Calendar.MINUTE);
        c.setTime(itemDate);
        int videoYear = c.get(Calendar.YEAR);
        int videoMonth = c.get(Calendar.MONTH);
        int videoDays = c.get(Calendar.DAY_OF_MONTH);
        int videoHour = c.get(Calendar.HOUR_OF_DAY);
        int videoMinute = c.get(Calendar.MINUTE);

        if (yearNow != videoYear) {
            return (String.valueOf(yearNow - videoYear) + "-" + "years");
        } else if (monthNow != videoMonth) {
            return (String.valueOf(monthNow - videoMonth) + "-" + "months");
        } else if (dayNow != videoDays) {
            return (String.valueOf(dayNow - videoDays) + "-" + "days");
        } else if (hourNow != videoHour) {
            return (String.valueOf(hourNow - videoHour) + "-" + "hours");
        } else if (minuteNow != videoMinute) {
            return (String.valueOf(minuteNow - videoMinute) + "-" + "minutes");
        } else {
            return "0-seconds";
        }
    }

    public static Date getDateFromString(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String dateInLocaleFormat(Date date){
        return new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd MMM yyyy"), Locale.getDefault()).format(date);
    }

    public static String formatMWDate(Date date) {
        return isoFormat.format(date);
    }
}
