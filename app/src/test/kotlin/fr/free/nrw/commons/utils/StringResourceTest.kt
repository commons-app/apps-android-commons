package fr.free.nrw.commons.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class StringResourceTest {
    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(qualifiers = "en-rUS")
    fun `valid placeholder is formatted`() {
        assertEquals(
            "Uploaded by: TestUploader",
            context().getStringSafe(R.string.image_uploaded_by, "TestUploader"),
        )
    }

    @Test
    @Config(qualifiers = "af")
    fun `broken Afrikaans placeholder falls back to English`() {
        assertEquals(
            "Uploaded by: TestUploader",
            context().getStringSafe(R.string.image_uploaded_by, "TestUploader"),
        )
    }

    @Test
    @Config(qualifiers = "ka")
    fun `broken Georgian upload title falls back to English`() {
        assertEquals(
            "File uploaded!",
            context().getStringSafe(R.string.upload_completed_notification_title, "File"),
        )
    }

    @Test
    @Config(qualifiers = "ka")
    fun `valid Georgian placeholder stays translated`() {
        assertEquals(
            "ფაილის ატვირთვა: File",
            context().getStringSafe(R.string.upload_progress_notification_title_start, "File"),
        )
    }
}
