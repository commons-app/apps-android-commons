package fr.free.nrw.commons.upload

import android.net.Uri
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.nearby.Place
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

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
    }

    @Test
    fun receiveMultipleItems() {
        val element = Mockito.mock(Uri::class.java)
        val element2 = Mockito.mock(Uri::class.java)
        var uriList: List<Uri> = mutableListOf<Uri>(element, element2)
        uploadPresenter!!.receive(uriList, "image/jpeg", "external", mock(Place::class.java))
    }
}