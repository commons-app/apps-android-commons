package fr.free.nrw.commons.utils

import java.util.Calendar
import java.util.Date

/**
 * Get the start date of wlm monument
 * For this release we are hardcoding it to be 1st September
 * @return
 */
const val wLMStartDate: String = "1 Sep"

/***
 * Get the end date of wlm monument
 * For this release we are hardcoding it to be 31st October
 * @return
 */
const val wLMEndDate: String = "30 Sep"

/**
 * For now we are enabling the monuments only when the date lies between 1 Sept & 31 OCt
 */
val isMonumentsEnabled: Boolean
    get() = Date().month == 8

/***
 * Function to get the current WLM year
 * It increments at the start of September in line with the other WLM functions
 * (No consideration of locales for now)
 * @param calendar
 * @return
 */
fun getWikiLovesMonumentsYear(calendar: Calendar): Int {
    var year = calendar[Calendar.YEAR]
    if (calendar[Calendar.MONTH] < Calendar.SEPTEMBER) {
        year -= 1
    }
    return year
}
