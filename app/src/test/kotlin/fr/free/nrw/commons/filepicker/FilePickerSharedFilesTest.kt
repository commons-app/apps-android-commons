package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

/**
 * Tests that files shared from other apps in a format Commons does not accept are skipped before
 * they are copied, see issue #6908.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class FilePickerSharedFilesTest {
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        Robolectric.setupContentProvider(FakeSharedFilesProvider::class.java, FakeSharedFilesProvider.AUTHORITY)
    }

    @Test
    fun rawFilesSharedWithTheirJpegsAreSkippedBeforeBeingCopied() {
        val jpegs = listOf(FakeSharedFilesProvider.uriFor("PXL_1.jpg"), FakeSharedFilesProvider.uriFor("PXL_2.jpg"))
        val raws = listOf(FakeSharedFilesProvider.uriFor("PXL_1.dng"), FakeSharedFilesProvider.uriFor("PXL_2.dng"))
        val jpegContents = jpegs.map { share(it) }
        val rawContents = raws.map { share(it) }

        val result =
            FilePicker.handleExternalImagesPicked(
                sendMultipleIntent(listOf(jpegs[0], raws[0], jpegs[1], raws[1])),
                activity,
            )

        assertEquals(jpegs, result.files.map { it.contentUri })
        assertEquals(2, result.skippedCount)
        assertTrue(jpegContents.all { it.wasRead })
        assertFalse(rawContents.any { it.wasRead })
    }

    @Test
    fun sharingOnlyRawFilesSkipsAllOfThem() {
        val raw = FakeSharedFilesProvider.uriFor("PXL_1.dng")
        val rawContent = share(raw)

        val result = FilePicker.handleExternalImagesPicked(sendMultipleIntent(listOf(raw)), activity)

        assertTrue(result.files.isEmpty())
        assertEquals(1, result.skippedCount)
        assertFalse(rawContent.wasRead)
    }

    private fun share(uri: Uri): TrackedContent =
        TrackedContent().also { shadowOf(activity.contentResolver).registerInputStream(uri, it) }

    private fun sendMultipleIntent(uris: List<Uri>): Intent {
        val clipData = ClipData.newRawUri(null, uris.first())
        uris.drop(1).forEach { clipData.addItem(ClipData.Item(it)) }
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            this.clipData = clipData
        }
    }

    /**
     * The content of a shared file, which records whether it was read, that is, copied.
     */
    private class TrackedContent : ByteArrayInputStream(ByteArray(16)) {
        var wasRead = false
            private set

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int {
            wasRead = true
            return super.read(b, off, len)
        }
    }
}
