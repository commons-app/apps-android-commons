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
    fun displayTextShouldNotBeUnderscored() {
        assertEquals("Ex 1", PageTitle("Ex_1").displayText)
        assertEquals("Ex 1", PageTitle("Ex_1 ").displayText)
        assertEquals("Ex 1", PageTitle("Ex 1").displayText)
        assertEquals("Ex 1", PageTitle("Ex 1 ").displayText)
        assertEquals("File:Ex 1 2", PageTitle("File:Ex_1_2 ").displayText)
    }

    @Test
    fun prefixedTextShouldBeUnderscored() {
        assertEquals("Ex_1", PageTitle("Ex_1").prefixedText)
        assertEquals("Ex_1", PageTitle("Ex_1 ").prefixedText)
        assertEquals("Ex_1", PageTitle("Ex 1").prefixedText)
        assertEquals("Ex_1", PageTitle("Ex 1 ").prefixedText)
        assertEquals("File:Ex_1_2", PageTitle("File:Ex 1 2 ").prefixedText)
    }

    @Test
    fun fileNameWithOneColon() {
        val pageTitle = PageTitle("File:sample:a.jpg")
        assertEquals("File:Sample:a.jpg", pageTitle.prefixedText)
        assertEquals("File:Sample:a.jpg", pageTitle.displayText)
        assertEquals("Sample:a.jpg", pageTitle.text)
    }

    @Test
    fun fileNameWithMoreThanOneColon() {
        var pageTitle = PageTitle("File:sample:a:b.jpg")
        assertEquals("File:Sample:a:b.jpg", pageTitle.prefixedText)
        assertEquals("File:Sample:a:b.jpg", pageTitle.displayText)
        assertEquals("Sample:a:b.jpg", pageTitle.text)

        pageTitle = PageTitle("File:sample:a:b:c.jpg")
        assertEquals("File:Sample:a:b:c.jpg", pageTitle.prefixedText)
        assertEquals("File:Sample:a:b:c.jpg", pageTitle.displayText)
        assertEquals("Sample:a:b:c.jpg", pageTitle.text)
    }

    @Test
    fun keyShouldNotIncludeNamespace() {
        val pageTitle = PageTitle("File:Sample.jpg")
        assertEquals("Sample.jpg", pageTitle.text)
    }

    @Test
    fun capitalizeNamespace() {
        val pageTitle = PageTitle("file:Sample.jpg")
        assertEquals("File:Sample.jpg", pageTitle.prefixedText)
        assertEquals("File:Sample.jpg", pageTitle.displayText)
    }

    @Test
    fun capitalizeKey() {
        val pageTitle = PageTitle("File:sample.jpg")
        assertEquals("File:Sample.jpg", pageTitle.prefixedText)
        assertEquals("File:Sample.jpg", pageTitle.displayText)
        assertEquals("Sample.jpg", pageTitle.text)
    }

    @Test
    fun getMobileUriForTest() {
        val pageTitle = PageTitle("Test")
        assertEquals(BuildConfig.MOBILE_HOME_URL + "Test", pageTitle.mobileUri.toString())
    }

    @Test
    fun spaceBecomesUnderscoreInUri() {
        val pageTitle = PageTitle("File:Ex 1.jpg")
        assertEquals(BuildConfig.HOME_URL + "File:Ex_1.jpg", pageTitle.canonicalUri.toString())
    }

    @Test
    fun leaveSubpageNamesUncapitalizedInUri() {
        val pageTitle = PageTitle("User:Ex/subpage")
        assertEquals(BuildConfig.HOME_URL + "User:Ex/subpage", pageTitle.canonicalUri.toString())
    }

    @Test
    fun unicodeUri() {
        val pageTitle = PageTitle("User:例")
        assertEquals(BuildConfig.HOME_URL + "User:" + URLEncoder.encode("例", "utf-8"), pageTitle.canonicalUri.toString())
    }
}
