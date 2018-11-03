package fr.free.nrw.commons

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLEncoder

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [21], application = TestCommonsApplication::class)
class PageTitleTest {
    @Test
    fun displayTitleShouldNotBeUnderscored() {
        assertEquals("Ex 1", PageTitle("Ex_1").displayTitle)
        assertEquals("Ex 1", PageTitle("Ex_1 ").displayTitle)
        assertEquals("Ex 1", PageTitle("Ex 1").displayTitle)
        assertEquals("Ex 1", PageTitle("Ex 1 ").displayTitle)
        assertEquals("File:Ex 1 2", PageTitle("File:Ex_1_2 ").displayTitle)
    }

    @Test
    fun prefixedTitleShouldBeUnderscored() {
        assertEquals("Ex_1", PageTitle("Ex_1").prefixedTitle)
        assertEquals("Ex_1", PageTitle("Ex_1 ").prefixedTitle)
        assertEquals("Ex_1", PageTitle("Ex 1").prefixedTitle)
        assertEquals("Ex_1", PageTitle("Ex 1 ").prefixedTitle)
        assertEquals("File:Ex_1_2", PageTitle("File:Ex 1 2 ").prefixedTitle)
    }

    @Test
    fun fileNameWithOneColon() {
        val pageTitle = PageTitle("File:sample:a.jpg")
        assertEquals("File:Sample:a.jpg", pageTitle.prefixedTitle)
        assertEquals("File:Sample:a.jpg", pageTitle.displayTitle)
        assertEquals("Sample:a.jpg", pageTitle.key)
        assertEquals("Sample:a.jpg", pageTitle.displayKey)
    }

    @Test
    fun fileNameWithMoreThanOneColon() {
        var pageTitle = PageTitle("File:sample:a:b.jpg")
        assertEquals("File:Sample:a:b.jpg", pageTitle.prefixedTitle)
        assertEquals("File:Sample:a:b.jpg", pageTitle.displayTitle)
        assertEquals("Sample:a:b.jpg", pageTitle.key)
        assertEquals("Sample:a:b.jpg", pageTitle.displayKey)

        pageTitle = PageTitle("File:sample:a:b:c.jpg")
        assertEquals("File:Sample:a:b:c.jpg", pageTitle.prefixedTitle)
        assertEquals("File:Sample:a:b:c.jpg", pageTitle.displayTitle)
        assertEquals("Sample:a:b:c.jpg", pageTitle.key)
        assertEquals("Sample:a:b:c.jpg", pageTitle.displayKey)
    }

    @Test
    fun keyShouldNotIncludeNamespace() {
        val pageTitle = PageTitle("File:Sample.jpg")
        assertEquals("Sample.jpg", pageTitle.key)
        assertEquals("Sample.jpg", pageTitle.displayKey)
    }

    @Test
    fun capitalizeNamespace() {
        val pageTitle = PageTitle("file:Sample.jpg")
        assertEquals("File:Sample.jpg", pageTitle.prefixedTitle)
        assertEquals("File:Sample.jpg", pageTitle.displayTitle)
    }

    @Test
    fun capitalizeKey() {
        val pageTitle = PageTitle("File:sample.jpg")
        assertEquals("File:Sample.jpg", pageTitle.prefixedTitle)
        assertEquals("File:Sample.jpg", pageTitle.displayTitle)
        assertEquals("Sample.jpg", pageTitle.key)
        assertEquals("Sample.jpg", pageTitle.displayKey)
    }

    @Test
    fun getMobileUriForTest() {
        val pageTitle = PageTitle("Test")
        assertEquals(BuildConfig.MOBILE_HOME_URL + "Test", pageTitle.mobileCommonsUri.toString())
    }

    @Test
    fun spaceBecomesUnderscoreInUri() {
        val pageTitle = PageTitle("File:Ex 1.jpg")
        assertEquals(BuildConfig.HOME_URL + "File:Ex_1.jpg", pageTitle.commonsURI.toString())
    }

    @Test
    fun leaveSubpageNamesUncapitalizedInUri() {
        val pageTitle = PageTitle("User:Ex/subpage")
        assertEquals(BuildConfig.HOME_URL + "User:Ex/subpage", pageTitle.commonsURI.toString())
    }

    @Test
    fun unicodeUri() {
        val pageTitle = PageTitle("User:例")
        assertEquals(BuildConfig.HOME_URL + "User:" + URLEncoder.encode("例", "utf-8"), pageTitle.commonsURI.toString())
    }
}
