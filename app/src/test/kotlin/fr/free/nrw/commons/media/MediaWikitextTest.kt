package fr.free.nrw.commons.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val MAX_CAPTION_LENGTH = 250
private const val ELLIPSIS = "..."
private const val WORD_BOUNDARY_PREFIX_LENGTH = MAX_CAPTION_LENGTH - ELLIPSIS.length
private const val REPEATED_PADDING_WORDS = 30
private const val EXTRA_UNBROKEN_CHARS = 270
private const val EXACT_DESCRIPTION_OVERFLOW = 260

/** Exercises the clipboard formatter with descriptions, whitespace, truncation, and captions. */
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
        assertEquals(
            "[[$title|thumb|SANY R285R excavator in Thailand]]",
            formatMediaWikitext(title, description),
        )
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

    /** Line breaks, tabs, and non-breaking spaces collapse into single spaces. */
    @Test
    fun collapsesLineBreaksAndNonBreakingSpaces() {
        val description =
            "<p>First paragraph.&nbsp;\t</p><p>Second\u00A0\u202Fparagraph.</p>\n\r\nThird\n\nline"
        assertEquals(
            "[[File:Example.jpg|thumb|First paragraph. Second paragraph. Third line]]",
            formatMediaWikitext("File:Example.jpg", description),
        )
    }

    /** Captions that collapse to at most 250 characters are kept in full without an ellipsis. */
    @Test
    fun keepsCaptionAtExactMaxLengthUntouched() {
        val exact = "a".repeat(MAX_CAPTION_LENGTH)
        val padded = "  $exact\n\n\u00A0 "
        assertEquals(
            "[[File:Example.jpg|thumb|$exact]]",
            formatMediaWikitext("File:Example.jpg", padded),
        )
    }

    /** Long descriptions are truncated at a word boundary within 250 characters with ellipsis. */
    @Test
    fun truncatesLongCaptionAtWordBoundary() {
        val words = List(REPEATED_PADDING_WORDS) { "paragraph" }.joinToString("\n\n")
        val result = formatMediaWikitext("File:Long.jpg", words)
        val caption = result.removePrefix("[[File:Long.jpg|thumb|").removeSuffix("]]")

        assertTrue(caption.endsWith("paragraph$ELLIPSIS"))
        assertTrue(caption.length <= MAX_CAPTION_LENGTH)

        val exactBoundary = "x".repeat(WORD_BOUNDARY_PREFIX_LENGTH) + " tail"
        val boundaryResult = formatMediaWikitext("File:Boundary.jpg", exactBoundary)
        val boundaryCaption =
            boundaryResult.removePrefix("[[File:Boundary.jpg|thumb|").removeSuffix("]]")
        assertEquals("x".repeat(WORD_BOUNDARY_PREFIX_LENGTH) + ELLIPSIS, boundaryCaption)
        assertEquals(MAX_CAPTION_LENGTH, boundaryCaption.length)
    }

    /** Unbroken words, entities near the cut, and surrogate pairs truncate safely. */
    @Test
    fun truncatesUnbrokenWordsEntitiesAndSurrogatesSafely() {
        val unbroken = "w".repeat(EXTRA_UNBROKEN_CHARS)
        val unbrokenCaption =
            formatMediaWikitext("File:Token.jpg", unbroken)
                .removePrefix("[[File:Token.jpg|thumb|")
                .removeSuffix("]]")
        assertEquals("w".repeat(WORD_BOUNDARY_PREFIX_LENGTH) + ELLIPSIS, unbrokenCaption)
        assertEquals(MAX_CAPTION_LENGTH, unbrokenCaption.length)

        val nearCutWithDelimiter =
            "a".repeat(WORD_BOUNDARY_PREFIX_LENGTH - 1) + "|tail " + "b".repeat(REPEATED_PADDING_WORDS)
        val escapedResult = formatMediaWikitext("File:Pipe.jpg", nearCutWithDelimiter)
        assertEquals(
            "[[File:Pipe.jpg|thumb|" + "a".repeat(WORD_BOUNDARY_PREFIX_LENGTH - 1) + "&#124;$ELLIPSIS]]",
            escapedResult,
        )

        val emojiPrefix = "b".repeat(WORD_BOUNDARY_PREFIX_LENGTH - 1) + "\uD83D\uDE00tail"
        val emojiCaption =
            formatMediaWikitext("File:Emoji.jpg", emojiPrefix)
                .removePrefix("[[File:Emoji.jpg|thumb|")
                .removeSuffix("]]")
        assertEquals("b".repeat(WORD_BOUNDARY_PREFIX_LENGTH - 1) + ELLIPSIS, emojiCaption)
    }

    /** Prefers the user's language structured caption and falls back to description otherwise. */
    @Test
    fun prefersStructuredCaptionInUserLanguageAndFallsBack() {
        val longDescription = "word ".repeat(EXACT_DESCRIPTION_OVERFLOW)
        val captions =
            mapOf(
                "en" to "English <caption\u00A0with & | brackets>",
                "fr" to "Légende française",
            )

        assertEquals(
            "[[File:Sample.jpg|thumb|English &lt;caption with &amp; &#124; brackets&gt;]]",
            formatMediaWikitext("File:Sample.jpg", longDescription, captions, language = "en"),
        )

        val blankUserCaption = mapOf("en" to " \n\u00A0 ", "fr" to "Légende française")
        val fallback =
            formatMediaWikitext(
                "File:Sample.jpg",
                "<p>Fallback\ndescription</p>",
                blankUserCaption,
                language = "en",
            )
        assertEquals("[[File:Sample.jpg|thumb|Fallback description]]", fallback)

        val missingUserLanguage =
            formatMediaWikitext("File:Sample.jpg", "Description text", mapOf("fr" to "Légende"), language = "de")
        assertEquals("[[File:Sample.jpg|thumb|Description text]]", missingUserLanguage)
    }
}
