package fr.free.nrw.commons.utils

import org.junit.Assert
import org.junit.Test

class CommonsDateUtilTest {

    @Test
    fun `Iso8601DateFormatTimestamp parses legal date`() {
        val iso8601DateFormatTimestamp = CommonsDateUtil
            .getIso8601DateFormatTimestamp()
        val parsedDate = iso8601DateFormatTimestamp
            .parse("2020-04-07T14:21:57Z")
        Assert.assertEquals(
            "2020-04-07T14:21:57Z",
            iso8601DateFormatTimestamp.format(parsedDate)
        )
    }
}
