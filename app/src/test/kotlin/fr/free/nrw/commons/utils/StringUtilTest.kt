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

}
