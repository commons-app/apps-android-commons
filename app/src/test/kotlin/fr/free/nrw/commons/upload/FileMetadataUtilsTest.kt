package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface.TAG_ARTIST
import androidx.exifinterface.media.ExifInterface.TAG_CAMARA_OWNER_NAME
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.util.*

/**
 * Test cases for FileMetadataUtils
 */
class FileMetadataUtilsTest {

    /**
     * Test method to verify EXIF tags
     */
    @Test
    fun getTagsFromPref() {
        val author = FileMetadataUtils.getTagsFromPref("Author")
        val authorRef = arrayOf(TAG_ARTIST, TAG_CAMARA_OWNER_NAME);

        assertTrue(Arrays.deepEquals(author, authorRef))
    }
}