package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

class UploadControllerTest {
    @Mock
    internal var context: Context? = null

    @Mock
    internal lateinit var store: JsonKvStore

    @Mock
    internal lateinit var contentResolver: ContentResolver

    @InjectMocks
    var uploadController: UploadController? = null

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun startUpload() {
        val contribution = mock(Contribution::class.java)
        val media = mock<Media>()
        whenever(contribution.media).thenReturn(media)
        whenever(media.author).thenReturn("Creator")
        whenever(context?.contentResolver).thenReturn(contentResolver)
        uploadController?.prepareMedia(contribution)
    }
}
