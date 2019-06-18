package fr.free.nrw.commons.upload

import android.content.SharedPreferences
import fr.free.nrw.commons.caching.CacheController
import fr.free.nrw.commons.mwapi.CategoryApi
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import javax.inject.Inject
import javax.inject.Named

class FileProcessorTest {

    @Mock
    internal var cacheController: CacheController? = null
    @Mock
    internal var gpsCategoryModel: GpsCategoryModel? = null
    @Mock
    internal var apiCall: CategoryApi? = null
    @Mock
    @field:[Inject Named("default_preferences")]
    internal var prefs: SharedPreferences? = null

    @InjectMocks
    var fileProcessor: FileProcessor? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun processFileCoordinates() {

    }

    /**
     * Test method to verify redaction Exif metadata
     */
    @Test
    fun redactExifTags() {
        /*
        val filePathRef: String? = "src/test/data/exif_redact_sample.jpg"
        val filePathTmp: String? = "" + System.getProperty("java.io.tmpdir") + "exif_redact_sample_tmp.jpg"

        val inStream = FileInputStream(filePathRef)
        val outStream = FileOutputStream(filePathTmp)
        val inChannel = inStream.getChannel()
        val outChannel = outStream.getChannel()
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inStream.close()
        outStream.close()

        val redactTags = mutableSetOf("Author", "Copyright", "Location", "Camera Model",
                "Lens Model", "Serial Numbers", "Software")

        val exifInterface : ExifInterface? = ExifInterface(filePathTmp.toString())

        var nonEmptyTag = false
        for (redactTag in redactTags) {
            for (tag in FileMetadataUtils.getTagsFromPref(redactTag)) {
                val tagValue = exifInterface?.getAttribute(tag)
                if(tagValue != null) {
                    nonEmptyTag = true
                    break
                }
            }
            if (nonEmptyTag) break
        }
        // all tags are empty, can't test redaction
        assert(nonEmptyTag)

        FileProcessor.redactExifTags(exifInterface, redactTags)

        for (redactTag in redactTags) {
            for (tag in FileMetadataUtils.getTagsFromPref(redactTag)) {
                val oldValue = exifInterface?.getAttribute(tag)
                assert(oldValue == null)
            }
        }
        */
    }
}