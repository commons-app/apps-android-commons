package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface.*
import org.junit.Test
import java.util.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.`is`

/**
 * Test cases for FileMetadataUtils
 */
class FileMetadataUtilsTest {

    /**
     * Test method to verify EXIF tags for "Author"
     */
    @Test
    fun getTagsFromPrefAuthor() {
        val author = FileMetadataUtils.getTagsFromPref("Author")
        val authorRef = arrayOf(TAG_ARTIST, TAG_CAMERA_OWNER_NAME);

        assertThat(Arrays.deepEquals(author, authorRef), `is`(true))
    }

    /**
     * Test method to verify EXIF tags for  "Location"
     */
    @Test
    fun getTagsFromPrefLocation() {
        val author = FileMetadataUtils.getTagsFromPref("Location")
        val authorRef = arrayOf(TAG_GPS_LATITUDE, TAG_GPS_LATITUDE_REF,
                TAG_GPS_LONGITUDE, TAG_GPS_LONGITUDE_REF,
                TAG_GPS_ALTITUDE, TAG_GPS_ALTITUDE_REF)

        assertThat(Arrays.deepEquals(author, authorRef), `is`(true))
    }

    /**
     * Test method to verify EXIF tags for  "Copyright"
     */
    @Test
    fun getTagsFromPrefCopyWright() {
        val author = FileMetadataUtils.getTagsFromPref("Copyright")
        val authorRef = arrayOf(TAG_COPYRIGHT)

        assertThat(Arrays.deepEquals(author, authorRef), `is`(true))
    }

    /**
     * Test method to verify EXIF tags for  "Camera Model"
     */
    @Test
    fun getTagsFromPrefCameraModel() {
        val author = FileMetadataUtils.getTagsFromPref("Camera Model")
        val authorRef = arrayOf(TAG_MAKE, TAG_MODEL)

        assertThat(Arrays.deepEquals(author, authorRef), `is`(true))
    }

    /**
     * Test method to verify EXIF tags for  "Lens Model"
     */
    @Test
    fun getTagsFromPrefLensModel() {
        val author = FileMetadataUtils.getTagsFromPref("Lens Model")
        val authorRef = arrayOf(TAG_LENS_MAKE, TAG_LENS_MODEL, TAG_LENS_SPECIFICATION)

        assertThat(Arrays.deepEquals(author, authorRef), `is`(true))
    }

    /**
     * Test method to verify EXIF tags for  "Serial Numbers"
     */
    @Test
    fun getTagsFromPrefSerialNumbers() {
        val author = FileMetadataUtils.getTagsFromPref("Serial Numbers")
        val authorRef = arrayOf(TAG_BODY_SERIAL_NUMBER, TAG_LENS_SERIAL_NUMBER)

        assertThat(Arrays.deepEquals(author, authorRef), `is`(true))
    }

    /**
     * Test method to verify EXIF tags for  "Software"
     */
    @Test
    fun getTagsFromPrefSoftware() {
        val author = FileMetadataUtils.getTagsFromPref("Software")
        val authorRef = arrayOf(TAG_SOFTWARE)

        assertThat(Arrays.deepEquals(author, authorRef), `is`(true))
    }
}