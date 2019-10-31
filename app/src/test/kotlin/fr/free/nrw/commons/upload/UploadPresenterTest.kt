package fr.free.nrw.commons.upload

import com.nhaarman.mockito_kotlin.verify
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.repository.UploadRepository
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.ArrayList


/**
 * The clas contains unit test cases for UploadPresenter
 */
class UploadPresenterTest {

    @Mock
    internal var repository: UploadRepository? = null
    @Mock
    internal var view: UploadContract.View? = null
    @Mock
    var contribution: Contribution? = null

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @InjectMocks
    var uploadPresenter: UploadPresenter? = null

    private var uploadableFiles: ArrayList<UploadableFile> = ArrayList()

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        uploadPresenter?.onAttachView(view)
        `when`(repository?.buildContributions()).thenReturn(Observable.just(contribution))
        `when`(view?.isLoggedIn).thenReturn(true)
        uploadableFiles.add(uploadableFile)
        `when`(view?.uploadableFiles).thenReturn(uploadableFiles)
        `when`(uploadableFile?.filePath).thenReturn("data://test")
    }

    /**
     * unit test case for method UploadPresenter.handleSubmit
     */
    @Test
    fun handleSubmitTest() {
        uploadPresenter?.handleSubmit()
        verify(view)?.isLoggedIn
        verify(view)?.showProgress(true)
        verify(repository)?.buildContributions()
        verify(repository)?.buildContributions()
    }

    /**
     * unit test for UploadMediaPresenter.deletePictureAtIndex
     */
    @Test
    fun deletePictureAtIndexTest() {
        uploadPresenter?.deletePictureAtIndex(0)
        verify(repository)?.deletePicture(ArgumentMatchers.anyString())
        verify(view)?.showMessage(ArgumentMatchers.anyInt())//As there is only one while which we are asking for deletion, upload should be cancelled and this flow should be triggered
        verify(view)?.finish()
    }
}