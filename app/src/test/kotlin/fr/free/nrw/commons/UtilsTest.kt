package fr.free.nrw.commons

import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import java.util.*

class UtilsTest {
    @Test
    fun wikiLovesMonumentsYearBeforeSeptember() {
        val cal = Calendar.getInstance()
        cal.set(2022, Calendar.FEBRUARY, 1)
        assertThat(2021, equalTo( Utils.getWikiLovesMonumentsYear(cal)))
    }

    @Test
    fun wikiLovesMonumentsYearInSeptember() {
        val cal = Calendar.getInstance()
        cal.set(2022, Calendar.SEPTEMBER, 1)
        assertThat(2022, equalTo( Utils.getWikiLovesMonumentsYear(cal)))
    }

    @Test
    fun wikiLovesMonumentsYearAfterSeptember() {
        val cal = Calendar.getInstance()
        cal.set(2022, Calendar.DECEMBER, 1)
        assertThat(2022, equalTo( Utils.getWikiLovesMonumentsYear(cal)))
    }
}