package fr.free.nrw.commons.upload

import fr.free.nrw.commons.contributions.UploadableFile
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
        `when`(uploadModel!!.preProcessImages(ArgumentMatchers.anyListOf(UploadableFile::class.java),
                ArgumentMatchers.any(Place::class.java),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(SimilarImageInterface::class.java)))
                .thenReturn(Observable.just(mock(UploadModel.UploadItem::class.java)))
    }

    @Test
    fun receiveMultipleItems() {
        val element = Mockito.mock(UploadableFile::class.java)
        val element2 = Mockito.mock(UploadableFile::class.java)
        var uriList: List<UploadableFile> = mutableListOf<UploadableFile>(element, element2)
        uploadPresenter!!.receive(uriList, "external", mock(Place::class.java))
    }
}