package fr.free.nrw.commons.upload

import android.content.ComponentName
import android.content.Context
import fr.free.nrw.commons.HandlerService
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

class UploadControllerTest {

    @Mock
    internal var sessionManager: SessionManager? = null
    @Mock
    internal var context: Context? = null
    @Mock
    internal var prefs: JsonKvStore? = null

    @InjectMocks
    var uploadController: UploadController? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val uploadService = mock(UploadService::class.java)
        val binder = mock(HandlerService.HandlerServiceLocalBinder::class.java)
        `when`(binder.service).thenReturn(uploadService)
        uploadController!!.uploadServiceConnection.onServiceConnected(mock(ComponentName::class.java), binder)
    }

    @Test
    fun prepareService() {
        uploadController!!.prepareService()
    }

    @Test
    fun cleanup() {
        uploadController!!.cleanup()
    }

    @Test
    fun startUpload() {
        val contribution = mock(Contribution::class.java)
        `when`(contribution.getAuthor()).thenReturn("Author")
        uploadController!!.startUpload(contribution)
    }
}