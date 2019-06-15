package fr.free.nrw.commons.upload

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
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations


/**
 * The class contains unit test cases for UploadMediaPresenter
 */
class UploadMediaPresenterTest {
    @Mock
    internal var repository: UploadRepository? = null
    @Mock
    internal var view: UploadMediaDetailsContract.View? = null

    private var uploadMediaPresenter: UploadMediaPresenter? = null

    @Mock
    private var uploadableFile: UploadableFile? = null

    @Mock
    private var place: Place? = null

    @Mock
    private var uploadItem: UploadModel.UploadItem? = null

    private var testObservableUploadItem: Observable<UploadModel.UploadItem>? = null
    private var testSingleImageResult: Single<Int>? = null

    private var testScheduler: TestScheduler? = null

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
        uploadMediaPresenter?.onAttachView(view)
    }

    /**
     * unit test for method UploadMediaPresenter.receiveImage
     */
    @Test
    fun receiveImageTest() {
        Mockito.`when`(repository?.preProcessImage(ArgumentMatchers.any(UploadableFile::class.java), ArgumentMatchers.any(Place::class.java), ArgumentMatchers.anyString(), ArgumentMatchers.any(UploadMediaPresenter::class.java))).thenReturn(testObservableUploadItem)
        uploadMediaPresenter?.receiveImage(uploadableFile, ArgumentMatchers.anyString(), place)
        verify(view)?.showProgress(true)
        testScheduler?.triggerActions()
        verify(view)?.onImageProcessed(ArgumentMatchers.any(UploadModel.UploadItem::class.java), ArgumentMatchers.any(Place::class.java))
        verify(view)?.showProgress(false)
    }

    /**
     * unit test for method UploadMediaPresenter.verifyImageQuality
     */
    @Test
    fun verifyImageQualityTest() {
        Mockito.`when`(repository?.getImageQuality(ArgumentMatchers.any(UploadModel.UploadItem::class.java), ArgumentMatchers.any(Boolean::class.java))).thenReturn(testSingleImageResult)
        Mockito.`when`(uploadItem?.imageQuality).thenReturn(ArgumentMatchers.anyInt())
        uploadMediaPresenter?.verifyImageQuality(uploadItem, true)
        verify(view)?.showProgress(true)
        testScheduler?.triggerActions()
        verify(view)?.showProgress(false)
    }

    /**
     * unit test for method UploadMediaPresenter.handleImageResult
     */
    @Test
    fun handleImageResult() {
        //Positive case test
        uploadMediaPresenter?.handleImageResult(IMAGE_KEEP)
        verify(view)?.onImageValidationSuccess()

        //Duplicate file name
        uploadMediaPresenter?.handleImageResult(FILE_NAME_EXISTS)
        verify(view)?.showDuplicatePicturePopup()

        //Empty Title test
        uploadMediaPresenter?.handleImageResult(EMPTY_TITLE)
        verify(view)?.showMessage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())

        //Bad Picture test
        //Empty Title test
        uploadMediaPresenter?.handleImageResult(-7)
        verify(view)?.showBadImagePopup(ArgumentMatchers.anyInt())

    }

}