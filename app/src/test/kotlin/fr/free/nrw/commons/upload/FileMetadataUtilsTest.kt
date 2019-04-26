package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface.*
import io.reactivex.Observable
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
        val author: Observable<String>? = FileMetadataUtils.getTagsFromPref("Author")
        val authorRef: Observable<String>? = Observable.fromArray(TAG_ARTIST, TAG_CAMARA_OWNER_NAME)

        assertTrue(Arrays.deepEquals(arrayOf(author?.toList()?.blockingGet()),
                arrayOf(authorRef?.toList()?.blockingGet())))
    }
}