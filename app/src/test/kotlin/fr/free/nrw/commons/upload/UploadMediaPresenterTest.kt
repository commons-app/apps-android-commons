package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.filepicker.UploadableFile
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
    private lateinit var uploadItem: UploadModel.UploadItem

    @Mock
    private lateinit var uploadMediaDetails: List<UploadMediaDetail>

    private lateinit var testObservableUploadItem: Observable<UploadModel.UploadItem>
    private lateinit var testSingleImageResult: Single<Int>

    private lateinit var testScheduler: TestScheduler

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
        uploadMediaPresenter = UploadMediaPresenter(repository, testScheduler, testScheduler)
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
            ArgumentMatchers.any(UploadModel.UploadItem::class.java),
            ArgumentMatchers.any(Place::class.java)
        )
        verify(view).showProgress(false)
    }

    /**
     * unit test for method UploadMediaPresenter.verifyImageQuality
     */
    @Test
    fun verifyImageQualityTest() {
        whenever(repository.getImageQuality(ArgumentMatchers.any(UploadModel.UploadItem::class.java)))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(ArgumentMatchers.anyInt())
        uploadMediaPresenter.verifyImageQuality(uploadItem)
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
        uploadMediaPresenter.handleImageResult(IMAGE_KEEP)
        verify(view).onImageValidationSuccess()

        //Duplicate file name
        uploadMediaPresenter.handleImageResult(FILE_NAME_EXISTS)
        verify(view).showDuplicatePicturePopup()

        //Empty Caption test
        uploadMediaPresenter.handleImageResult(EMPTY_CAPTION)
        verify(view).showMessage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())

        //Bad Picture test
        //Empty Caption test
        uploadMediaPresenter.handleImageResult(-7)
        verify(view)?.showBadImagePopup(ArgumentMatchers.anyInt())

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
            uploadMediaPresenter.setUploadItem(0, uploadItem)
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
            uploadMediaPresenter.setUploadItem(0, uploadItem)
        }
    }

    /**
     * Test fetch previous image title when there was one
     */
    @Test
    fun fetchPreviousImageAndTitleTestPositive() {
        whenever(repository.getPreviousUploadItem(ArgumentMatchers.anyInt()))
            .thenReturn(uploadItem)
        whenever(uploadItem.uploadMediaDetails).thenReturn(uploadMediaDetails)

        uploadMediaPresenter.fetchPreviousTitleAndDescription(0)
        verify(view).setCaptionsAndDescriptions(ArgumentMatchers.any())
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
        uploadMediaPresenter.handleBadImage(8)
        verify(repository).saveValue(ArgumentMatchers.anyString(), eq(false))
        verify(view).showBadImagePopup(8)
    }

    /**
     * Test bad image empty title
     */
    @Test
    fun handleBadImageBaseTestEmptyTitle() {
        uploadMediaPresenter.handleBadImage(-3)
        verify(view).showMessage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())
    }

    /**
     * Teste show file already exists
     */
    @Test
    fun handleBadImageBaseTestFileNameExists() {
        uploadMediaPresenter.handleBadImage(-4)
        verify(view).showDuplicatePicturePopup()
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

    /**
     * Test set upload item
     */
    @Test
    fun setUploadItemTest() {
        uploadMediaPresenter.setUploadItem(0, uploadItem)
        verify(repository).updateUploadItem(0, uploadItem)
    }

}
