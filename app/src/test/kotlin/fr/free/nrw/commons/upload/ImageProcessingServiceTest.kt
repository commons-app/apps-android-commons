package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ImageUtils
import fr.free.nrw.commons.utils.ImageUtilsWrapper
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.FileInputStream
import java.io.InputStream

class ImageProcessingServiceTest {
    @Mock
    internal var fileUtilsWrapper: FileUtilsWrapper? = null

    @Mock
    internal var imageUtilsWrapper: ImageUtilsWrapper? = null

    @Mock
    internal var readFBMD: ReadFBMD? = null

    @Mock
    internal var readEXIF: EXIFReader? = null

    @Mock
    internal var mediaClient: MediaClient? = null

    @Mock
    internal var location: LatLng? = null

    @Mock
    internal lateinit var appContext: Context

    @InjectMocks
    var imageProcessingService: ImageProcessingService? = null

    @Mock
    internal lateinit var uploadItem: UploadItem

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val mediaUri = mock(Uri::class.java)
        val contentUri = mock(Uri::class.java)
        val mockPlace = mock(Place::class.java)
        val mockTitle = mock(List::class.java)
        val contentResolver = mock(ContentResolver::class.java)

        `when`(mockPlace.wikiDataEntityId).thenReturn("Q1")
        `when`(mockPlace.getLocation()).thenReturn(mock(LatLng::class.java))
        `when`(mediaUri.path).thenReturn("filePath")
        /*`when`(mockTitle.isEmpty).thenReturn(false)
        `when`(mockTitle.isSet).thenReturn(true)*/

        `when`(uploadItem.mediaUri).thenReturn(mediaUri)
        `when`(uploadItem.contentUri).thenReturn(contentUri)
        `when`(appContext.contentResolver).thenReturn(contentResolver)
        `when`(uploadItem.imageQuality).thenReturn(ImageUtils.IMAGE_WAIT)

        `when`(uploadItem.uploadMediaDetails).thenReturn(mockTitle as MutableList<UploadMediaDetail>?)

        `when`(uploadItem.place).thenReturn(mockPlace)
        `when`(uploadItem.filename).thenReturn("File:jpg")

        `when`(fileUtilsWrapper!!.getFileInputStream(ArgumentMatchers.anyString()))
            .thenReturn(mock(FileInputStream::class.java))
        `when`(appContext.contentResolver.openInputStream(ArgumentMatchers.any()))
            .thenReturn(mock(InputStream::class.java))
        `when`(fileUtilsWrapper!!.getSHA1(any(InputStream::class.java)))
            .thenReturn("fileSha")

        `when`(fileUtilsWrapper!!.getGeolocationOfFile(ArgumentMatchers.anyString(), any(LatLng::class.java)))
            .thenReturn("latLng")

        `when`(imageUtilsWrapper?.checkIfImageIsTooDark(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(ImageUtils.IMAGE_OK))

        `when`(imageUtilsWrapper!!.checkImageGeolocationIsDifferent(ArgumentMatchers.anyString(), any(LatLng::class.java)))
            .thenReturn(Single.just(ImageUtils.IMAGE_OK))

        `when`(fileUtilsWrapper!!.getFileInputStream(ArgumentMatchers.anyString()))
            .thenReturn(mock(FileInputStream::class.java))
        `when`(fileUtilsWrapper!!.getSHA1(any(FileInputStream::class.java)))
            .thenReturn("fileSha")
        `when`(mediaClient!!.checkFileExistsUsingSha(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(false))
        `when`(mediaClient?.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(false))
        `when`(readFBMD?.processMetadata(ArgumentMatchers.any()))
            .thenReturn(Single.just(ImageUtils.IMAGE_OK))
        `when`(readEXIF?.processMetadata(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(ImageUtils.IMAGE_OK))
    }

    @Test
    fun validateImageForKeepImage() {
        `when`(uploadItem.imageQuality).thenReturn(ImageUtils.IMAGE_KEEP)
        val validateImage = imageProcessingService!!.validateImage(uploadItem, location)
        assertEquals(ImageUtils.IMAGE_OK, validateImage.blockingGet())
    }

    @Test
    fun validateImageForDuplicateImage() {
        `when`(mediaClient!!.checkFileExistsUsingSha(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(true))
        val validateImage = imageProcessingService!!.validateImage(uploadItem, location)
        assertEquals(ImageUtils.IMAGE_DUPLICATE, validateImage.blockingGet())
    }

    @Test
    fun validateImageForOkImage() {
        val validateImage = imageProcessingService!!.validateImage(uploadItem, location)
        assertEquals(ImageUtils.IMAGE_OK, validateImage.blockingGet())
    }

    @Test
    fun validateImageForDarkImage() {
        `when`(imageUtilsWrapper?.checkIfImageIsTooDark(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(ImageUtils.IMAGE_DARK))
        val validateImage = imageProcessingService!!.validateImage(uploadItem, location)
        assertEquals(ImageUtils.IMAGE_DARK, validateImage.blockingGet())
    }

    @Test
    fun validateImageForWrongGeoLocation() {
        `when`(imageUtilsWrapper!!.checkImageGeolocationIsDifferent(ArgumentMatchers.anyString(), any(LatLng::class.java)))
            .thenReturn(Single.just(ImageUtils.IMAGE_GEOLOCATION_DIFFERENT))
        val validateImage = imageProcessingService!!.validateImage(uploadItem, location)
        assertEquals(ImageUtils.IMAGE_GEOLOCATION_DIFFERENT, validateImage.blockingGet())
    }

    @Test
    fun validateImageForFileNameExistsWithCheckTitleOn() {
        `when`(mediaClient?.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(true))
        val validateImage = imageProcessingService!!.validateCaption(uploadItem)
        assertEquals(ImageUtils.FILE_NAME_EXISTS, validateImage.blockingGet())
    }
}
