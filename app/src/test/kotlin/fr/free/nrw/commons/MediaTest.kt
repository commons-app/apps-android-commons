package fr.free.nrw.commons

import media
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MediaTest {
    @Test
    fun displayTitleShouldStripExtension() {
        val m = media(filename = "File:Example.jpg")
        assertEquals("Example", m.displayTitle)
    }

    @Test
    fun displayTitleShouldUseSpaceForUnderscore() {
        val m = media(filename = "File:Example 1_2.jpg")
        assertEquals("Example 1 2", m.displayTitle)
    }

    @Test
    fun testGetDisplayAuthorWithCompleteHtmlTags() {
        // Test with author containing complete html tags
        val media = media(author = "<b>John Doe</b>")
        assertEquals("John Doe", media.getDisplayAuthor())
    }

    @Test
    fun testGetDisplayAuthorWithHalfHtmlTag() {
        // Test with author containing half html tag
        val media = media(author = "<a href='example.com'>John Doe")
        assertEquals("John Doe", media.getDisplayAuthor())
    }

    @Test
    fun testGetDisplayAuthorWithoutHtmlTags() {
        // Test with author containing no html tags
        val media = media(author = "John Doe")
        assertEquals("John Doe", media.getDisplayAuthor())
    }

    @Test
    fun testGetDisplayAuthorWithMultipleHtmlTags() {
        // Test with author containing multiple html tags
        val media = media(author = "<b><i>John Doe</i></b>")
        assertEquals("", media.getDisplayAuthor())
    }

    @Test
    fun testGetDisplayAuthorWithEmptyAuthor() {
        // Test with empty author
        val media = media(author = null)
        assertEquals("", media.getDisplayAuthor())
    }
}


