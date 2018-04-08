package fr.free.nrw.commons.utils

import fr.free.nrw.commons.utils.StringSortingUtils.sortBySimilarity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections.sort

class StringSortingUtilsTest {

    @Test
    fun testSortingNumbersBySimilarity() {
        val actualList = listOf("1234567", "4567", "12345", "123", "1234")
        val expectedList = listOf("1234", "12345", "123", "1234567", "4567")

        sort(actualList, sortBySimilarity("1234"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingTextBySimilarity() {
        val actualList = listOf("The quick brown fox",
                "quick brown fox",
                "The",
                "The quick ",
                "The fox",
                "brown fox",
                "fox")
        val expectedList = listOf("The",
                "The fox",
                "The quick ",
                "The quick brown fox",
                "quick brown fox",
                "brown fox",
                "fox")

        sort(actualList, sortBySimilarity("The"))

        assertEquals(expectedList, actualList)
    }
}