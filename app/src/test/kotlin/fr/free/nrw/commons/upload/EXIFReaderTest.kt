package fr.free.nrw.commons.upload

import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.utils.ImageUtils.FILE_NO_EXIF
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class EXIFReaderTest {
    private val exifReader = EXIFReader()

    @Test
    fun `processMetadata returns FILE_NO_EXIF when the file cannot be read`() {
        Assert.assertEquals(FILE_NO_EXIF, exifReader.processMetadata("").blockingGet())
    }
}
