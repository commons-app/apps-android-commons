package fr.free.nrw.commons.upload

import android.net.Uri
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter
import fr.free.nrw.commons.utils.ImageUtils.*
import io.github.coordinates2country.Coordinates2Country
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import java.util.*


/**
 * The class contains unit test cases for UploadMediaPresenter
 */
@RunWith(RobolectricTestRunner::class)
@PrepareForTest(Coordinates2Country::class)
class UploadMediaPresenterTest {
    @Mock
    internal lateinit var repository: UploadRepository

    @Mock
    internal lateinit var view: UploadMediaDetailsContract.View

    private lateinit var uploadMediaPresenter: UploadMediaPresenter

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @Mock
    private lateinit var place: Place

    @Mock
    private var location: LatLng? = null

    @Mock
    private lateinit var uploadItem: UploadItem

    @Mock
    private lateinit var imageCoordinates: ImageCoordinates

    @Mock
    private lateinit var uploadMediaDetails: List<UploadMediaDetail>

    private lateinit var testObservableUploadItem: Observable<UploadItem>
    private lateinit var testSingleImageResult: Single<Int>

    private lateinit var testScheduler: TestScheduler
    private lateinit var mockedCountry: MockedStatic<Coordinates2Country>

    @Mock
    private lateinit var jsonKvStore: JsonKvStore

    @Mock
    lateinit var mockActivity: UploadActivity

    /**
     * initial setup unit test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testObservableUploadItem = Observable.just(uploadItem)
        testSingleImageResult = Single.just(1)
        testScheduler = TestScheduler()
        uploadMediaPresenter = UploadMediaPresenter(
            repository, jsonKvStore, testScheduler, testScheduler
        )
        uploadMediaPresenter.onAttachView(view)
        mockedCountry = mockStatic(Coordinates2Country::class.java)
    }

    @After
    fun tearDown() {
        mockedCountry.close()
    }


    /**
     * unit test for method UploadMediaPresenter.receiveImage
     */
    @Test
    fun receiveImageTest() {
        whenever(
            repository.preProcessImage(
                ArgumentMatchers.any(UploadableFile::class.java),
                ArgumentMatchers.any(Place::class.java),
                ArgumentMatchers.any(UploadMediaPresenter::class.java),
                ArgumentMatchers.any(LatLng::class.java)
            )
        ).thenReturn(testObservableUploadItem)
        uploadMediaPresenter.receiveImage(uploadableFile, place, location)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
        verify(view).onImageProcessed(
            ArgumentMatchers.any(UploadItem::class.java),
            ArgumentMatchers.any(Place::class.java)
        )
    }

    /**
     * unit test for method UploadMediaPresenter.getImageQuality (For else case)
     */
    @Test
    fun getImageQualityTest() {
        whenever(repository.uploads).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(0)
        whenever(uploadItem.gpsCoords)
            .thenReturn(imageCoordinates)
        whenever(uploadItem.gpsCoords.decimalCoords)
            .thenReturn("imageCoordinates")
        uploadMediaPresenter.getImageQuality(0, location, mockActivity)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
    }

    /**
     * unit test for method UploadMediaPresenter.getImageQuality (For if case)
     */
    @Test
    fun `get ImageQuality Test while coordinates equals to null`() {
        whenever(repository.uploads).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(0)
        whenever(uploadItem.gpsCoords)
            .thenReturn(imageCoordinates)
        whenever(uploadItem.gpsCoords.decimalCoords)
            .thenReturn(null)
        uploadMediaPresenter.getImageQuality(0, location, mockActivity)
        testScheduler.triggerActions()
    }

    /**
     * Test for empty file name when the user presses the NEXT button
     */
    @Test
    fun emptyFileNameTest() {
        uploadMediaPresenter.handleCaptionResult(EMPTY_CAPTION, uploadItem);
        verify(view).showMessage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())
    }

    /**
     * Test for duplicate file name when the user presses the NEXT button
     */
    @Test
    fun duplicateFileNameTest() {
        uploadMediaPresenter.handleCaptionResult(FILE_NAME_EXISTS, uploadItem)
        verify(view).showDuplicatePicturePopup(uploadItem)
    }

    /**
     * Test for correct file name when the user presses the NEXT button
     */
    @Test
    fun correctFileNameTest() {
        uploadMediaPresenter.handleCaptionResult(IMAGE_OK, uploadItem)
        verify(view).onImageValidationSuccess()
    }

    @Test
    fun addSingleCaption() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.captionText = "added caption"
        uploadMediaDetail.languageCode = "en"
        val uploadMediaDetailList: ArrayList<UploadMediaDetail> = ArrayList()
        uploadMediaDetailList.add(uploadMediaDetail)
        uploadItem.setMediaDetails(uploadMediaDetailList)
        Mockito.`when`(repository.getImageQuality(uploadItem, location)).then {
            verify(view).showProgress(true)
            testScheduler.triggerActions()
            verify(view).showProgress(true)
            verify(view).onImageValidationSuccess()
        }
    }

    @Test
    fun addMultipleCaptions() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.captionText = "added caption"
        uploadMediaDetail.languageCode = "en"
        uploadMediaDetail.captionText = "added caption"
        uploadMediaDetail.languageCode = "eo"
        uploadItem.setMediaDetails(Collections.singletonList(uploadMediaDetail))
        Mockito.`when`(repository.getImageQuality(uploadItem, location)).then {
            verify(view).showProgress(true)
            testScheduler.triggerActions()
            verify(view).showProgress(true)
            verify(view).onImageValidationSuccess()
        }
    }

    /**
     * Test fetch image title when there was one
     */
    @Test
    fun fetchImageAndTitleTest() {
        whenever(repository.uploads).thenReturn(listOf(uploadItem))
        whenever(repository.getUploadItem(ArgumentMatchers.anyInt()))
            .thenReturn(uploadItem)
        whenever(uploadItem.uploadMediaDetails).thenReturn(listOf())

        uploadMediaPresenter.fetchTitleAndDescription(0)
        verify(view).updateMediaDetails(ArgumentMatchers.any())
    }

    /**
     * Test show SimilarImageFragment
     */
    @Test
    fun showSimilarImageFragmentTest() {
        val similar: ImageCoordinates = mock()
        uploadMediaPresenter.showSimilarImageFragment("original", "possible", similar)
        verify(view).showSimilarImageFragment("original", "possible", similar)
    }

    @Test
    fun setCorrectCountryCodeForReceivedImage() {

        val germanyAsPlace =
            Place(null, null, null, null, LatLng(50.1, 10.2, 1.0f), null, null, null, true)
        germanyAsPlace.isMonument = true

        whenever(
            Coordinates2Country.country(
                ArgumentMatchers.eq(germanyAsPlace.getLocation().latitude),
                ArgumentMatchers.eq(germanyAsPlace.getLocation().longitude)
            )
        ).thenReturn("Germany")

        val item: Observable<UploadItem> =
            Observable.just(UploadItem(Uri.EMPTY, null, null, germanyAsPlace, 0, null, null, null))

        whenever(
            repository.preProcessImage(
                ArgumentMatchers.any(UploadableFile::class.java),
                ArgumentMatchers.any(Place::class.java),
                ArgumentMatchers.any(UploadMediaPresenter::class.java),
                ArgumentMatchers.any(LatLng::class.java)
            )
        ).thenReturn(item)

        uploadMediaPresenter.receiveImage(uploadableFile, germanyAsPlace, location)
        verify(view).showProgress(true)
        testScheduler.triggerActions()

        val captor: ArgumentCaptor<UploadItem> = ArgumentCaptor.forClass(UploadItem::class.java)
        verify(view).onImageProcessed(
            captor.capture(),
            ArgumentMatchers.any(Place::class.java)
        )

        assertEquals("Exptected contry code", "de", captor.value.countryCode);
    }
}
