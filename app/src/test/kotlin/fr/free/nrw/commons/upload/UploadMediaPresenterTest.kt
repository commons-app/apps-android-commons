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
        uploadMediaPresenter = UploadMediaPresenter(repository,
            jsonKvStore,testScheduler, testScheduler)
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
        verify(view).showProgress(false)
    }

    /**
     * unit test for method UploadMediaPresenter.verifyImageQuality (For else case)
     */
    @Test
    fun verifyImageQualityTest() {
        whenever(repository.uploads).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(0)
        whenever(uploadItem.gpsCoords)
            .thenReturn(imageCoordinates)
        whenever(uploadItem.gpsCoords.decimalCoords)
            .thenReturn("imageCoordinates")
        uploadMediaPresenter.verifyImageQuality(0, location)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
        verify(view).showProgress(false)
    }

    /**
     * unit test for method UploadMediaPresenter.verifyImageQuality (For if case)
     */
    @Test
    fun `verify ImageQuality Test while coordinates equals to null`() {
        whenever(repository.uploads).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(0)
        whenever(uploadItem.gpsCoords)
            .thenReturn(imageCoordinates)
        whenever(uploadItem.gpsCoords.decimalCoords)
            .thenReturn(null)
        uploadMediaPresenter.verifyImageQuality(0, location)
        testScheduler.triggerActions()
    }

    /**
     * unit test for method UploadMediaPresenter.handleImageResult
     */
    @Test
    fun handleImageResult() {
        //Positive case test
        uploadMediaPresenter.handleImageResult(IMAGE_KEEP, uploadItem)
        verify(view).onImageValidationSuccess()

        //Duplicate file name
        uploadMediaPresenter.handleImageResult(FILE_NAME_EXISTS, uploadItem)
        verify(view).showDuplicatePicturePopup(uploadItem)

        //Empty Caption test
        uploadMediaPresenter.handleImageResult(EMPTY_CAPTION, uploadItem)
        verify(view).showMessage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())

        // Bad Picture Test
        uploadMediaPresenter.handleImageResult(-7, uploadItem)
        verify(view)?.showBadImagePopup(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(uploadItem))
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
     * Test bad image invalid location
     */
    @Test
    fun handleBadImageBaseTestInvalidLocation() {
        uploadMediaPresenter.handleBadImage(8, uploadItem)
        verify(view).showBadImagePopup(8, uploadItem)
    }

    /**
     * Test bad image empty title
     */
    @Test
    fun handleBadImageBaseTestEmptyTitle() {
        uploadMediaPresenter.handleBadImage(-3, uploadItem)
        verify(view).showMessage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())
    }

    /**
     * Teste show file already exists
     */
    @Test
    fun handleBadImageBaseTestFileNameExists() {
        uploadMediaPresenter.handleBadImage(64, uploadItem)
        verify(view).showDuplicatePicturePopup(uploadItem)
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

        val germanyAsPlace = Place(null,null, null, null, LatLng(50.1, 10.2, 1.0f), null, null, null, true)
        germanyAsPlace.isMonument = true

        whenever(
            Coordinates2Country.country(
                ArgumentMatchers.eq(germanyAsPlace.getLocation().latitude),
                ArgumentMatchers.eq(germanyAsPlace.getLocation().longitude)
            )
        ).thenReturn("Germany")

        val item: Observable<UploadItem> = Observable.just(UploadItem(Uri.EMPTY, null, null, germanyAsPlace, 0, null, null, null))

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

        verify(view).showProgress(false)
    }
}
