package fr.free.nrw.commons.upload

import android.content.ComponentName
import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
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
    }

    @Test
    fun prepareService() {
    }

    @Test
    fun cleanup() {
    }

    @Test
    fun startUpload() {
        val contribution = mock(Contribution::class.java)
        val media = mock<Media>()
        whenever(contribution.media).thenReturn(media)
        whenever(media.author).thenReturn("Creator")
        uploadController!!.prepareMedia(contribution)
    }
}
