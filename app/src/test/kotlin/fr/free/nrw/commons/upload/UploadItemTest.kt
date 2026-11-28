package fr.free.nrw.commons.upload

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UploadItemTest {
    private fun itemWithCaption(caption: String): UploadItem {
        val item = UploadItem(Uri.EMPTY, "image/jpeg", null, null, 0, null, null, null)
        item.uploadMediaDetails[0].captionText = caption
        return item
    }

    @Test
    fun `file name is the caption with the extension`() {
        assertEquals("Line 1-2.jpg", itemWithCaption("Line 1-2").filename)
    }

    @Test
    fun `file name has a slash replaced like Commons does when saving the upload`() {
        assertEquals("Line 1-2.jpg", itemWithCaption("Line 1/2").filename)
    }

    @Test
    fun `file name has every character that Commons replaces turned into a dash`() {
        assertEquals("a-b-c-d-e-f-g-h-i-j-k.jpg", itemWithCaption("a#b<c>d[e]f|g{h}i:j/k").filename)
        assertEquals("a-b.jpg", itemWithCaption("a\\b").filename)
    }
}
