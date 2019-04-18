package fr.free.nrw.commons.upload

import android.app.Application
import android.content.Context
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.reactivex.Single
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
import java.util.*
import javax.inject.Inject
import javax.inject.Named


class UploadModelTest {

    @Mock
    @field:[Inject Named("licenses")]
    internal var licenses: List<String>? = null
    @Mock
    @field:[Inject Named("default_preferences")]
    internal var prefs: JsonKvStore? = null
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
    internal var fileProcessor: FileProcessor? = null
    @Mock
    internal var imageProcessingService: ImageProcessingService? = null

    @InjectMocks
    var uploadModel: UploadModel? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        `when`(context!!.applicationContext)
                .thenReturn(mock(Application::class.java))
        `when`(fileUtilsWrapper!!.getFileExt(anyString()))
                .thenReturn("jpg")
        `when`(fileUtilsWrapper!!.getSHA1(any(InputStream::class.java)))
                .thenReturn("sha")
        `when`(fileUtilsWrapper!!.getFileInputStream(anyString()))
                .thenReturn(mock(FileInputStream::class.java))
        `when`(fileUtilsWrapper!!.getGeolocationOfFile(anyString()))
                .thenReturn("")
        `when`(imageProcessingService!!.validateImage(any(UploadModel.UploadItem::class.java), anyBoolean()))
                .thenReturn(Single.just(IMAGE_OK))

    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    fun receive() {
        val preProcessImages = uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        preProcessImages.doOnComplete {
            assertTrue(uploadModel!!.items.size == 2)
        }
    }

    @Test
    fun verifyPreviousNotAvailable() {
        uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        assertFalse(uploadModel!!.isPreviousAvailable)
    }

    @Test
    fun verifyNextAvailable() {
        uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        assertTrue(uploadModel!!.isNextAvailable)
    }

    @Test
    fun isSubmitAvailable() {
        uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        assertTrue(uploadModel!!.isNextAvailable)
    }

    @Test
    fun getCurrentStep() {
        uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        assertTrue(uploadModel!!.currentStep == 1)
    }

    @Test
    fun getStepCount() {
        val preProcessImages = uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        preProcessImages.doOnComplete {
            assertTrue(uploadModel!!.stepCount == 4)
        }
    }

    @Test
    fun getCount() {
        val preProcessImages = uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        preProcessImages.doOnComplete {
            assertTrue(uploadModel!!.count == 2)
        }
    }

    @Test
    fun getUploads() {
        val preProcessImages = uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        preProcessImages.doOnComplete {
            assertTrue(uploadModel!!.uploads.size == 2)
        }
    }

    @Test
    fun isTopCardState() {
        uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        assertTrue(uploadModel!!.isTopCardState)
    }

    @Test
    fun next() {
        uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        assertTrue(uploadModel!!.currentStep == 1)
        uploadModel!!.next()
        assertTrue(uploadModel!!.currentStep == 2)
    }

    @Test
    fun previous() {
        uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        assertTrue(uploadModel!!.currentStep == 1)
        uploadModel!!.next()
        assertTrue(uploadModel!!.currentStep == 2)
        uploadModel!!.previous()
        assertTrue(uploadModel!!.currentStep == 1)
    }

    @Test
    fun isShowingItem() {
        val preProcessImages = uploadModel!!.preProcessImages(getMediaList(), mock(Place::class.java), "external") { _, _ -> }
        preProcessImages.doOnComplete {
            assertTrue(uploadModel!!.isShowingItem)
        }
    }

    private fun getMediaList(): List<UploadableFile> {
        val element = getElement()
        val element2 = getElement()
        var uriList: List<UploadableFile> = mutableListOf(element, element2)
        return uriList
    }

    private fun getElement(): UploadableFile {
        val mock = mock(UploadableFile::class.java)
        `when`(mock.filePath).thenReturn(UUID.randomUUID().toString() + "/filePath.jpg")
        return mock
    }

    @Test
    fun buildContributions() {
    }
}