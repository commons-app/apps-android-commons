package fr.free.nrw.commons.upload

import com.nhaarman.mockito_kotlin.verify
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.nearby.Place
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations


class UploadPresenterTest {

    @Mock
    internal var uploadModel: UploadModel? = null
    @Mock
    internal var uploadController: UploadController? = null
    @Mock
    internal var view: IUpload.View? = null
    @Mock
    var contribution: Contribution? = null

    @InjectMocks
    var uploadPresenter: UploadPresenter? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        uploadPresenter!!.onAttachView(view)
        `when`(uploadModel!!.preProcessImages(ArgumentMatchers.anyListOf(UploadableFile::class.java),
                ArgumentMatchers.any(Place::class.java),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(SimilarImageInterface::class.java)))
                .thenReturn(Observable.just(mock(UploadModel.UploadItem::class.java)))

        `when`(uploadModel!!.buildContributions()).thenReturn(Observable.just(contribution))
        `when`(view!!.isLoggedIn).thenReturn(true)
    }

    /*@Test
    fun receiveMultipleItems() {
        val element = Mockito.mock(UploadableFile::class.java)
        val element2 = Mockito.mock(UploadableFile::class.java)
        var uriList: List<UploadableFile> = mutableListOf<UploadableFile>(element, element2)
        uploadPresenter!!.receive(uriList, "external", mock(Place::class.java))
    }
*/
    @Test
    fun handleSubmitTest() {
        uploadPresenter!!.handleSubmit()
        verify(view!!).isLoggedIn
        verify(view!!).showProgress(true)
        verify(uploadModel!!).buildContributions()
        val buildContributions = uploadModel!!.buildContributions()
        buildContributions.test().assertNoErrors().assertValue {
            verify(uploadController!!).prepareService()
            verify(view!!).showProgress(false)
            verify(view!!).showMessage(ArgumentMatchers.any(Int::class.java))
            verify(view!!).finish()
            true
        }
    }
}