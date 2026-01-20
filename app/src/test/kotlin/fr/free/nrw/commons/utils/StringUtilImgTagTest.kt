package fr.free.nrw.commons.utils

import android.text.style.ImageSpan
import androidx.core.text.getSpans
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StringUtilImgTagTest {

    @Test
    fun `img tags are removed and do not create ImageSpan sqaure`() {
        val input = "Media <img src='x'> Commons"

        val result = StringUtil.fromHtml(input)

        // Text should remain, and <img> should not produce a placeholder sqaure.
        assertEquals("Media  Commons", result.toString())
        assertTrue(result.getSpans<ImageSpan>().isEmpty())
    }
    @Test
    fun `self closing img tags are removed and do not create ImageSpan sqaure`() {
        val input = "Media <img src='x'/> Commons"

        val result = StringUtil.fromHtml(input)

        assertEquals("Media  Commons", result.toString())
        assertTrue(result.getSpans<ImageSpan>().isEmpty())
    }

}
