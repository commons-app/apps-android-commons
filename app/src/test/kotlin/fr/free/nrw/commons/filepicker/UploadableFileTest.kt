package fr.free.nrw.commons.filepicker

import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class UploadableFileTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /**
     * The EXIF date string goes into the file page as it is, so it must use Western digits even
     * when the device language writes numbers differently
     */
    @Test
    fun getFileCreatedDateWritesExifDateWithWesternDigits() {
        val image = temporaryFolder.newFile("photo.jpg")
        File(SAMPLE_IMAGE).copyTo(image, overwrite = true)
        ExifInterface(image.absolutePath).apply {
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2023:11:07 10:30:00")
            saveAttributes()
        }

        val defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("bn"))
        try {
            val dateTime = UploadableFile(image).getFileCreatedDate(ApplicationProvider.getApplicationContext())

            assertEquals(UploadableFile.DateTimeWithSource.EXIF_SOURCE, dateTime?.source)
            assertEquals("2023-11-07", dateTime?.dateString)
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    private companion object {
        const val SAMPLE_IMAGE = "src/test/resources/ImageTest/ok2.jpg"
    }
}
