package fr.free.nrw.commons.upload

import android.net.Uri
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.nearby.Place
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class UploadPresenterTest {

    @Mock
    internal var uploadModel: UploadModel? = null
    @Mock
    internal var uploadController: UploadController? = null
    @Mock
    internal var mediaWikiApi: MediaWikiApi? = null

    @InjectMocks
    var uploadPresenter: UploadPresenter? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(uploadModel!!.preProcessImages(ArgumentMatchers.anyListOf(Uri::class.java),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(Place::class.java),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(SimilarImageInterface::class.java)))
                .thenReturn(Observable.just(mock(UploadModel.UploadItem::class.java)))
    }

    @Test
    fun receiveMultipleItems() {
        val element = Mockito.mock(Uri::class.java)
        val element2 = Mockito.mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadPresenter!!.receive(uriList, "image/jpeg", "external", mock(Place::class.java))
    }
}