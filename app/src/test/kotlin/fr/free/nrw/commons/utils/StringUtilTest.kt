package fr.free.nrw.commons.utils

import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.core.text.getSpans
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StringUtilTest {
    @Test
    fun `plain text remains plain text`() {
        val actual = StringUtil.fromHtml("foo bar")
        assertEquals("foo bar", actual.toString())
        assertArrayEquals(arrayOf<Object>(), actual.getSpans<Object>())
    }

    @Test
    fun `italicized text has StyleSpan`() {
        val actual = StringUtil.fromHtml("foo <i>bar</i>")
        val spans = actual.getSpans<StyleSpan>()
        assertEquals("foo bar", actual.toString())
        assertEquals(1, spans.size)
        assertEquals(Typeface.ITALIC, spans[0].style)
        assertEquals(4, actual.getSpanStart(spans[0]))
        assertEquals(7, actual.getSpanEnd(spans[0]))
    }

    @Test
    fun `unescape ampersand`() {
        val actual = StringUtil.fromHtml("foo &amp; bar")
        assertEquals("foo & bar", actual.toString())
        assertArrayEquals(arrayOf<Object>(), actual.getSpans<Object>())
    }

    @Test
    fun `lines written with a colon each start on a new line`() {
        // Description HTML of File:Arremon abeillei - Black-capped Sparrow XC250489.mp3
        val actual =
            StringUtil.fromHtml(
                ":First 8 s natural, rest after playback. Dense arid scrub\n" +
                    "<dl><dd>bird-seen:yes</dd>\n" +
                    "<dd>playback-used:yes</dd>\n" +
                    "<dd><b>Common name:</b> Black-capped Sparrow</dd>\n" +
                    "<dd><b>Type:</b> song</dd></dl>",
            )
        assertEquals(
            ":First 8 s natural, rest after playback. Dense arid scrub\n" +
                "bird-seen:yes\n" +
                "playback-used:yes\n" +
                "Common name: Black-capped Sparrow\n" +
                "Type: song",
            actual.toString(),
        )
    }

    @Test
    fun `term written with a semicolon is bold and on its own line`() {
        val actual = StringUtil.fromHtml("<dl><dt>Location</dt>\n<dd>Ecuador</dd></dl>")
        val spans = actual.getSpans<StyleSpan>()
        assertEquals("Location\nEcuador", actual.toString())
        assertEquals(1, spans.size)
        assertEquals(Typeface.BOLD, spans[0].style)
        assertEquals(0, actual.getSpanStart(spans[0]))
        assertEquals(8, actual.getSpanEnd(spans[0]))
    }

    @Test
    fun `nested lists and text after the list do not add empty lines`() {
        val actual =
            StringUtil.fromHtml(
                "<dl><dd>one\n<dl><dd>two</dd></dl></dd>\n<dd>three</dd></dl>\nafter",
            )
        assertEquals("one\ntwo\nthree\nafter", actual.toString())
    }
}
