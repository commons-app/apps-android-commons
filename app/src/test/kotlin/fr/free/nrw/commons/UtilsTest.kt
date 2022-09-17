package fr.free.nrw.commons

import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.util.*

class UtilsTest {
    @Test
    fun wikiLovesMonumentsYearBeforeSeptember() {
        val cal = Calendar.getInstance()
        cal.set(2022, Calendar.FEBRUARY, 1)
        Assertions.assertEquals(2021, Utils.getWikiLovesMonumentsYear(cal))
    }

    @Test
    fun wikiLovesMonumentsYearInSeptember() {
        val cal = Calendar.getInstance()
        cal.set(2022, Calendar.SEPTEMBER, 1)
        Assertions.assertEquals(2022, Utils.getWikiLovesMonumentsYear(cal))
    }

    @Test
    fun wikiLovesMonumentsYearAfterSeptember() {
        val cal = Calendar.getInstance()
        cal.set(2022, Calendar.DECEMBER, 1)
        Assertions.assertEquals(2022, Utils.getWikiLovesMonumentsYear(cal))
    }
}