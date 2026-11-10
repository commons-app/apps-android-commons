package fr.free.nrw.commons.media

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Exercises the clipboard formatter with the description shapes reported in issue #5075. */
@RunWith(RobolectricTestRunner::class)
class MediaWikitextTest {
    /** The plain description and the namespaced file title retain their Unicode characters. */
    @Test
    fun preservesPlainUnicodeDescription() {
        val title = "File:Nikola Šubić Zrinski Square.jpg"
        assertEquals(
            "[[$title|thumb|Nikola Šubić Zrinski Square (square)]]",
            formatMediaWikitext(title, "Nikola Šubić Zrinski Square (square)"),
        )
    }

    /** The nested Structured Data edit icon contributes no caption text. */
    @Test
    fun removesStructuredDataEditIcon() {
        val title = "File:SANY R285R excavator in Thailand.jpg"
        val description =
            "SANY R285R excavator in Thailand&nbsp;" +
                """<span class="noprint"><a href="#ooui-php-4">""" +
                """<img alt="Edit this at Structured Data on Commons" src="icon.png"></a></span>"""
        assertEquals("[[$title|thumb|SANY R285R excavator in Thailand]]", formatMediaWikitext(title, description))
    }

    /** Link text stays readable and encoded separators cannot split the file link. */
    @Test
    fun preservesLinkTextAndEscapesWikitextDelimiters() {
        val description =
            """Statue of <a href="/wiki/Nikola_Tesla">Nikola Tesla</a>""" +
                " &amp; Zagreb &#124; &#91;detail&#93;"
        assertEquals(
            "[[File:Statue.jpg|thumb|Statue of Nikola Tesla &amp; Zagreb &#124; &#91;detail&#93;]]",
            formatMediaWikitext("File:Statue.jpg", description),
        )
    }

    /** Missing descriptions produce a thumbnail without a caption parameter. */
    @Test
    fun omitsMissingDescription() {
        assertEquals("[[File:Example.jpg|thumb]]", formatMediaWikitext("File:Example.jpg", null))
    }
}
