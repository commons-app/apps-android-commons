package fr.free.nrw.commons.upload

import android.graphics.BitmapRegionDecoder
import android.net.Uri
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.BitmapRegionDecoderWrapper
import fr.free.nrw.commons.utils.ImageUtils
import fr.free.nrw.commons.utils.ImageUtilsWrapper
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.FileInputStream

class ImageProcessingServiceTest {
    @Mock
    internal var fileUtilsWrapper: FileUtilsWrapper? = null
    @Mock
    internal var bitmapRegionDecoderWrapper: BitmapRegionDecoderWrapper? = null
    @Mock
    internal var imageUtilsWrapper: ImageUtilsWrapper? = null
    @Mock
    internal var mwApi: MediaWikiApi? = null

    @InjectMocks
    var imageProcessingService: ImageProcessingService? = null

    @Mock
    internal lateinit var uploadItem: UploadModel.UploadItem

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val mediaUri = mock(Uri::class.java)
        val mockPlace = mock(Place::class.java)
        val mockTitle = mock(Title::class.java)

        `when`(mockPlace.wikiDataEntityId).thenReturn("Q1")
        `when`(mockPlace.getLocation()).thenReturn(mock(LatLng::class.java))
        `when`(mediaUri.path).thenReturn("filePath")
        `when`(mockTitle.isEmpty).thenReturn(false)
        `when`(mockTitle.isSet).thenReturn(true)

        `when`(uploadItem.mediaUri).thenReturn(mediaUri)
        `when`(uploadItem.imageQuality).thenReturn(ImageUtils.IMAGE_WAIT)

        `when`(uploadItem.title).thenReturn(mockTitle)

        `when`(uploadItem.place).thenReturn(mockPlace)

        `when`(fileUtilsWrapper!!.getFileInputStream(ArgumentMatchers.anyString()))
                .thenReturn(mock(FileInputStream::class.java))
        `when`(fileUtilsWrapper!!.getSHA1(any(FileInputStream::class.java)))
                .thenReturn("fileSha")

        `when`(fileUtilsWrapper!!.getGeolocationOfFile(ArgumentMatchers.anyString()))
                .thenReturn("latLng")

        `when`(bitmapRegionDecoderWrapper!!.newInstance(any(FileInputStream::class.java), anyBoolean()))
                .thenReturn(mock(BitmapRegionDecoder::class.java))
        `when`(imageUtilsWrapper!!.checkIfImageIsTooDark(any(BitmapRegionDecoder::class.java)))
                .thenReturn(Single.just(ImageUtils.IMAGE_OK))

        `when`(imageUtilsWrapper!!.checkImageGeolocationIsDifferent(ArgumentMatchers.anyString(), any(LatLng::class.java)))
                .thenReturn(Single.just(ImageUtils.IMAGE_OK))

        `when`(fileUtilsWrapper!!.getFileInputStream(ArgumentMatchers.anyString()))
                .thenReturn(mock(FileInputStream::class.java))
        `when`(fileUtilsWrapper!!.getSHA1(any(FileInputStream::class.java)))
                .thenReturn("fileSha")
        `when`(mwApi!!.existingFile(ArgumentMatchers.anyString()))
                .thenReturn(false)
        `when`(mwApi!!.fileExistsWithName(ArgumentMatchers.anyString()))
                .thenReturn(false)
    }

    @Test
    fun validateImageForKeepImage() {
        `when`(uploadItem.imageQuality).thenReturn(ImageUtils.IMAGE_KEEP)
        val validateImage = imageProcessingService!!.validateImage(uploadItem, false)
        assertEquals(ImageUtils.IMAGE_OK, validateImage.blockingGet())
    }

    @Test
    fun validateImageForDuplicateImage() {
        `when`(mwApi!!.existingFile(ArgumentMatchers.anyString()))
                .thenReturn(true)
        val validateImage = imageProcessingService!!.validateImage(uploadItem, false)
        assertEquals(ImageUtils.IMAGE_DUPLICATE, validateImage.blockingGet())
    }

    @Test
    fun validateImageForOkImage() {
        val validateImage = imageProcessingService!!.validateImage(uploadItem, false)
        assertEquals(ImageUtils.IMAGE_OK, validateImage.blockingGet())
    }

    @Test
    fun validateImageForDarkImage() {
        `when`(imageUtilsWrapper!!.checkIfImageIsTooDark(any(BitmapRegionDecoder::class.java)))
                .thenReturn(Single.just(ImageUtils.IMAGE_DARK))
        val validateImage = imageProcessingService!!.validateImage(uploadItem, false)
        assertEquals(ImageUtils.IMAGE_DARK, validateImage.blockingGet())
    }

    @Test
    fun validateImageForWrongGeoLocation() {
        `when`(imageUtilsWrapper!!.checkImageGeolocationIsDifferent(ArgumentMatchers.anyString(), any(LatLng::class.java)))
                .thenReturn(Single.just(ImageUtils.IMAGE_GEOLOCATION_DIFFERENT))
        val validateImage = imageProcessingService!!.validateImage(uploadItem, false)
        assertEquals(ImageUtils.IMAGE_GEOLOCATION_DIFFERENT, validateImage.blockingGet())
    }

    @Test
    fun validateImageForFileNameExistsWithCheckTitleOff() {
        `when`(mwApi!!.fileExistsWithName(ArgumentMatchers.anyString()))
                .thenReturn(true)
        val validateImage = imageProcessingService!!.validateImage(uploadItem, false)
        assertEquals(ImageUtils.IMAGE_OK, validateImage.blockingGet())
    }

    @Test
    fun validateImageForFileNameExistsWithCheckTitleOn() {
        `when`(mwApi!!.fileExistsWithName(ArgumentMatchers.nullable(String::class.java)))
                .thenReturn(true)
        val validateImage = imageProcessingService!!.validateImage(uploadItem, true)
        assertEquals(ImageUtils.FILE_NAME_EXISTS, validateImage.blockingGet())
    }
}