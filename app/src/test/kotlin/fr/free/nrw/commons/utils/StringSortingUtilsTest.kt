package fr.free.nrw.commons.utils

import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.utils.StringSortingUtils.sortBySimilarity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections.sort

class StringSortingUtilsTest {

    @Test
    fun testSortingNumbersBySimilarity() {
        val actualList = listOf(
            CategoryItem("1234567", "", "", false),
            CategoryItem("4567", "", "", false),
            CategoryItem("12345", "", "", false),
            CategoryItem("123", "", "", false),
            CategoryItem("1234", "", "", false))
        val expectedList = listOf(
            CategoryItem("1234", "", "", false),
            CategoryItem("12345", "", "", false),
            CategoryItem("123", "", "", false),
            CategoryItem("1234567", "", "", false),
            CategoryItem("4567", "", "", false))

        sort(actualList, sortBySimilarity("1234"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingTextBySimilarity() {
        val actualList = listOf(
            CategoryItem("The quick brown fox", "", "", false),
            CategoryItem("quick brown fox", "", "", false),
            CategoryItem("The", "", "", false),
            CategoryItem("The quick ", "", "", false),
            CategoryItem("The fox", "", "", false),
            CategoryItem("brown fox", "", "", false),
            CategoryItem("fox", "", "", false)
        )
        val expectedList = listOf(
            CategoryItem("The", "", "", false),
            CategoryItem("The fox", "", "", false),
            CategoryItem("The quick ", "", "", false),
            CategoryItem("The quick brown fox", "", "", false),
            CategoryItem("quick brown fox", "", "", false),
            CategoryItem("brown fox", "", "", false),
            CategoryItem("fox", "", "", false)
        )

        sort(actualList, sortBySimilarity("The"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingSymbolsBySimilarity() {
        val actualList = listOf(
            CategoryItem("$$$$$", "", "", false),
            CategoryItem("****", "", "", false),
            CategoryItem("**$*", "", "", false),
            CategoryItem("*$*$", "", "", false),
            CategoryItem(".*$", "", "", false)
        )
        val expectedList = listOf(
            CategoryItem("**$*", "", "", false),
            CategoryItem("*$*$", "", "", false),
            CategoryItem(".*$", "", "", false),
            CategoryItem("****", "", "", false),
            CategoryItem("$$$$$", "", "", false)
        )

        sort(actualList, sortBySimilarity("**$"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingMixedStringsBySimilarity() {
        // Sample from Category:2018 Android phones
        val actualList = listOf(
            CategoryItem("ASUS ZenFone 5 (2018)", "", "", false),
            CategoryItem("Google Pixel 3", "", "", false),
            CategoryItem("HTC U12", "", "", false),
            CategoryItem("Huawei P20", "", "", false),
            CategoryItem("LG G7 ThinQ", "", "", false),
            CategoryItem("Samsung Galaxy A8 (2018)", "", "", false),
            CategoryItem("Samsung Galaxy S9", "", "", false),
                // One with more complicated symbols
            CategoryItem("MadeUpPhone 2018.$£#你好", "", "", false)
        )
        val expectedList = listOf(
            CategoryItem("Samsung Galaxy S9", "", "", false),
            CategoryItem("ASUS ZenFone 5 (2018)", "", "", false),
            CategoryItem("Samsung Galaxy A8 (2018)", "", "", false),
            CategoryItem("Google Pixel 3", "", "", false),
            CategoryItem("HTC U12", "", "", false),
            CategoryItem("Huawei P20", "", "", false),
            CategoryItem("LG G7 ThinQ", "", "", false),
            CategoryItem("MadeUpPhone 2018.$£#你好", "", "", false)
        )

        sort(actualList, sortBySimilarity("S9"))

        assertEquals(expectedList, actualList)
    }

    @Test
    fun testSortingWithEmptyStrings() {
        val actualList = listOf(
            CategoryItem("brown fox", "", "", false),
            CategoryItem("", "", "", false),
            CategoryItem("quick brown fox", "", "", false),
            CategoryItem("the", "", "", false),
            CategoryItem("", "", "", false),
            CategoryItem("the fox", "", "", false),
            CategoryItem("fox", "", "", false),
            CategoryItem("", "", "", false),
            CategoryItem("", "", "", false)
        )
        val expectedList = listOf(
            CategoryItem("the fox", "", "", false),
            CategoryItem("brown fox", "", "", false),
            CategoryItem("the", "", "", false),
            CategoryItem("fox", "", "", false),
            CategoryItem("quick brown fox", "", "", false),
            CategoryItem("", "", "", false),
            CategoryItem("", "", "", false),
            CategoryItem("", "", "", false),
            CategoryItem("", "", "", false)
        )

        sort(actualList, sortBySimilarity("the fox"))

        assertEquals(expectedList, actualList)
    }
}