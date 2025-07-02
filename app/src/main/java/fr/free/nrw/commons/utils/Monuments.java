package fr.free.nrw.commons.utils;

import java.util.Calendar;
import java.util.Date;

public final class Monuments {

    /**
     * For now we are enabling the monuments only when the date lies between 1 Sept & 31 OCt
     * @param date
     * @return
     */
    public static boolean isMonumentsEnabled(final Date date) {
        if (date.getMonth() == 8) {
            return true;
        }
        return false;
    }

    /**
     * Util function to get the start date of wlm monument
     * For this release we are hardcoding it to be 1st September
     * @return
     */
    public static String getWLMStartDate() {
        return "1 Sep";
    }

    /***
     * Util function to get the end date of wlm monument
     * For this release we are hardcoding it to be 31st October
     * @return
     */
    public static String getWLMEndDate() {
        return "30 Sep";
    }

    /***
     * Function to get the current WLM year
     * It increments at the start of September in line with the other WLM functions
     * (No consideration of locales for now)
     * @param calendar
     * @return
     */
    public static int getWikiLovesMonumentsYear(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        if (calendar.get(Calendar.MONTH) < Calendar.SEPTEMBER) {
            year -= 1;
        }
        return year;
    }
}
