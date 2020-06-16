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
        val actualList = listOf(
            "The quick brown fox",
            "quick brown fox",
            "The",
            "The quick ",
            "The fox",
            "brown fox",
            "fox"
        )
        val expectedList = listOf(
            "The",
            "The fox",
            "The quick ",
            "The quick brown fox",
            "quick brown fox",
            "brown fox",
            "fox"
        )

        sort(actualList, sortBySimilarity("The"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingSymbolsBySimilarity() {
        val actualList = listOf(
            "$$$$$",
            "****",
            "**$*",
            "*$*$",
            ".*$"
        )
        val expectedList = listOf(
            "**$*",
            "*$*$",
            ".*$",
            "****",
            "$$$$$"
        )

        sort(actualList, sortBySimilarity("**$"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingMixedStringsBySimilarity() {
        // Sample from Category:2018 Android phones
        val actualList = listOf(
            "ASUS ZenFone 5 (2018)",
            "Google Pixel 3",
            "HTC U12",
            "Huawei P20",
            "LG G7 ThinQ",
            "Samsung Galaxy A8 (2018)",
            "Samsung Galaxy S9",
            // One with more complicated symbols
            "MadeUpPhone 2018.$£#你好"
        )
        val expectedList = listOf(
            "Samsung Galaxy S9",
            "ASUS ZenFone 5 (2018)",
            "Samsung Galaxy A8 (2018)",
            "Google Pixel 3",
            "HTC U12",
            "Huawei P20",
            "LG G7 ThinQ",
            "MadeUpPhone 2018.$£#你好"
        )

        sort(actualList, sortBySimilarity("S9"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingWithEmptyStrings() {
        val actualList = listOf(
            "brown fox",
            "",
            "quick brown fox",
            "the",
            "",
            "the fox",
            "fox",
            "",
            ""
        )
        val expectedList = listOf(
            "the fox",
            "brown fox",
            "the",
            "fox",
            "quick brown fox",
            "",
            "",
            "",
            ""
        )

        sort(actualList, sortBySimilarity("the fox"))

        assertEquals(expectedList, actualList)
    }
}
