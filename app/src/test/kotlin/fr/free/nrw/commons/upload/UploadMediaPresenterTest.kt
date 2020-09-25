package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter
import fr.free.nrw.commons.utils.ImageUtils.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.*


/**
 * The class contains unit test cases for UploadMediaPresenter
 */
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
    private lateinit var uploadItem: UploadItem

    @Mock
    private lateinit var uploadMediaDetails: List<UploadMediaDetail>

    private lateinit var testObservableUploadItem: Observable<UploadItem>
    private lateinit var testSingleImageResult: Single<Int>

    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var jsonKvStore: JsonKvStore

    /**
     * initial setup unit test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testObservableUploadItem = Observable.just(uploadItem)
        testSingleImageResult = Single.just(1)
        testScheduler = TestScheduler()
        uploadMediaPresenter = UploadMediaPresenter(repository,
            jsonKvStore,testScheduler, testScheduler)
        uploadMediaPresenter.onAttachView(view)
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
                ArgumentMatchers.any(UploadMediaPresenter::class.java)
            )
        ).thenReturn(testObservableUploadItem)
        uploadMediaPresenter.receiveImage(uploadableFile, place)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
        verify(view).onImageProcessed(
            ArgumentMatchers.any(UploadItem::class.java),
            ArgumentMatchers.any(Place::class.java)
        )
        verify(view).showProgress(false)
    }

    /**
     * unit test for method UploadMediaPresenter.verifyImageQuality
     */
    @Test
    fun verifyImageQualityTest() {
        whenever(repository.uploads).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(ArgumentMatchers.anyInt())
        uploadMediaPresenter.verifyImageQuality(0)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
        verify(view).showProgress(false)
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

        //Bad Picture test
        //Empty Caption test
        uploadMediaPresenter.handleImageResult(-7, uploadItem)
        verify(view)?.showBadImagePopup(ArgumentMatchers.anyInt(), eq(uploadItem))

    }

    @Test
    fun addSingleCaption() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.captionText = "added caption"
        uploadMediaDetail.languageCode = "en"
        val uploadMediaDetailList: ArrayList<UploadMediaDetail> = ArrayList()
        uploadMediaDetailList.add(uploadMediaDetail)
        uploadItem.setMediaDetails(uploadMediaDetailList)
        Mockito.`when`(repository.getImageQuality(uploadItem)).then {
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
        Mockito.`when`(repository.getImageQuality(uploadItem)).then {
            verify(view).showProgress(true)
            testScheduler.triggerActions()
            verify(view).showProgress(true)
            verify(view).onImageValidationSuccess()
        }
    }

    /**
     * Test fetch previous image title when there was one
     */
    @Test
    fun fetchPreviousImageAndTitleTestPositive() {
        whenever(repository.uploads).thenReturn(listOf(uploadItem))
        whenever(repository.getPreviousUploadItem(ArgumentMatchers.anyInt()))
            .thenReturn(uploadItem)
        whenever(uploadItem.uploadMediaDetails).thenReturn(listOf())

        uploadMediaPresenter.fetchPreviousTitleAndDescription(0)
        verify(view).updateMediaDetails(ArgumentMatchers.any())
    }

    /**
     * Test fetch previous image title when there was none
     */
    @Test
    fun fetchPreviousImageAndTitleTestNegative() {
        whenever(repository.getPreviousUploadItem(ArgumentMatchers.anyInt()))
            .thenReturn(null)
        uploadMediaPresenter.fetchPreviousTitleAndDescription(0)
        verify(view).showMessage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())
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
        uploadMediaPresenter.handleBadImage(-4, uploadItem)
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

}
