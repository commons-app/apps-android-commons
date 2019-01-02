package fr.free.nrw.commons.upload

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.utils.BitmapRegionDecoderWrapper
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import fr.free.nrw.commons.utils.ImageUtilsWrapper
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named


class UploadModelTest {

    @Mock
    @field:[Inject Named("licenses")]
    internal var licenses: List<String>? = null
    @Mock
    @field:[Inject Named("default_preferences")]
    internal var prefs: SharedPreferences? = null
    @Mock
    @field:[Inject Named("licenses_by_name")]
    internal var licensesByName: Map<String, String>? = null
    @Mock
    internal var context: Context? = null
    @Mock
    internal var mwApi: MediaWikiApi? = null
    @Mock
    internal var sessionManage: SessionManager? = null
    @Mock
    internal var fileUtilsWrapper: FileUtilsWrapper? = null
    @Mock
    internal var imageUtilsWrapper: ImageUtilsWrapper? = null
    @Mock
    internal var bitmapRegionDecoderWrapper: BitmapRegionDecoderWrapper? = null
    @Mock
    internal var fileProcessor: FileProcessor? = null

    @InjectMocks
    var uploadModel: UploadModel? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        `when`(context!!.applicationContext)
                .thenReturn(mock(Application::class.java))
        `when`(fileUtilsWrapper!!.createCopyPathAndCopy(anyBoolean(), any(Uri::class.java), nullable(ContentResolver::class.java), any(Context::class.java)))
                .thenReturn("file.jpg")
        `when`(fileUtilsWrapper!!.getFileExt(anyString()))
                .thenReturn("jpg")
        `when`(fileUtilsWrapper!!.getSHA1(any(InputStream::class.java)))
                .thenReturn("sha")
        `when`(fileUtilsWrapper!!.getFileInputStream(anyString()))
                .thenReturn(mock(FileInputStream::class.java))
        `when`(fileUtilsWrapper!!.getGeolocationOfFile(anyString()))
                .thenReturn("")
        `when`(imageUtilsWrapper!!.checkIfImageIsTooDark(any(BitmapRegionDecoder::class.java)))
                .thenReturn(IMAGE_OK)
        `when`(imageUtilsWrapper!!.checkImageGeolocationIsDifferent(anyString(), anyString()))
                .thenReturn(false)
        `when`(bitmapRegionDecoderWrapper!!.newInstance(any(FileInputStream::class.java), anyBoolean()))
                .thenReturn(mock(BitmapRegionDecoder::class.java))

    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    fun receive() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.items.size == 2)
    }

    @Test
    fun receiveDirect() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.items.size == 1)
    }

    @Test
    fun verifyPreviousNotAvailableForDirectUpload() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertFalse(uploadModel!!.isPreviousAvailable)
    }

    @Test
    fun verifyNextAvailableForDirectUpload() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.isNextAvailable)
    }

    @Test
    fun verifyPreviousNotAvailable() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertFalse(uploadModel!!.isPreviousAvailable)
    }

    @Test
    fun verifyNextAvailable() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.isNextAvailable)
    }

    @Test
    fun isSubmitAvailable() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.isNextAvailable)
    }

    @Test
    fun isSubmitAvailableForDirectUpload() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.isNextAvailable)
    }

    @Test
    fun getCurrentStepForDirectUpload() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.currentStep == 1)
    }

    @Test
    fun getCurrentStep() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.currentStep == 1)
    }

    @Test
    fun getStepCount() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.stepCount == 4)
    }

    @Test
    fun getStepCountForDirectUpload() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.stepCount == 3)
    }

    @Test
    fun getDirectCount() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.count == 1)
    }

    @Test
    fun getCount() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.count == 2)
    }

    @Test
    fun getUploads() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.uploads.size == 2)
    }

    @Test
    fun getDirectUploads() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.uploads.size == 1)
    }

    @Test
    fun isTopCardState() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.isTopCardState)
    }

    @Test
    fun isTopCardStateForDirectUpload() {
        val element = mock(Uri::class.java)
        uploadModel!!.receiveDirect(element, "image/jpeg", "external", "Q1", "Test", "Test", { _, _ -> }
                , "")
        assertTrue(uploadModel!!.isTopCardState)
    }

    @Test
    fun next() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.currentStep == 1)
        uploadModel!!.next()
        assertTrue(uploadModel!!.currentStep == 2)
    }

    @Test
    fun previous() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.currentStep == 1)
        uploadModel!!.next()
        assertTrue(uploadModel!!.currentStep == 2)
        uploadModel!!.previous()
        assertTrue(uploadModel!!.currentStep == 1)
    }

    @Test
    fun isShowingItem() {
        val element = mock(Uri::class.java)
        val element2 = mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadModel!!.receive(uriList, "image/jpeg", "external") { _, _ -> }
        assertTrue(uploadModel!!.isShowingItem)
    }

    @Test
    fun buildContributions() {
    }
}