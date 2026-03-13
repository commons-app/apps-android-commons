import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import fr.free.nrw.commons.upload.worker.UploadWorker

class UploadWorkerTest {

    @Mock
    lateinit var mediaClient: MediaClient

    @Mock
    lateinit var context: android.content.Context

    private lateinit var uploadWorker: UploadWorker

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        uploadWorker = mock<UploadWorker>()
        uploadWorker.mediaClient = mediaClient
        whenever(uploadWorker.mediaClient).thenReturn(mediaClient)
        whenever(uploadWorker.findUniqueFileName(any())).thenCallRealMethod()
    }

    @Test
    fun `findUniqueFileName returns original when file does not exist`() {
        val fileName = "test_image.jpg"
        whenever(mediaClient.checkPageExistsUsingTitle("File:$fileName"))
            .thenReturn(Single.just(false))

        val result = uploadWorker.findUniqueFileName(fileName)

        assertEquals(fileName, result)
    }

    @Test
    fun `findUniqueFileName appends hash when file exists`() {
        val fileName = "test_image.jpg"
        //mockking: first check exists, second check unique
        whenever(mediaClient.checkPageExistsUsingTitle(any()))
            .thenReturn(Single.just(true))
            .thenReturn(Single.just(false))

        val result = uploadWorker.findUniqueFileName(fileName)

        //herre, updated to match the exactly 3 digits inside parentheses
        val regex = Regex("test_image \\(\\d{3}\\)\\.jpg")
        assert(result.matches(regex))
    }

    @Test
    fun `findUniqueFileName handles files without extensions`() {
        val fileName = "no_extension"
        whenever(mediaClient.checkPageExistsUsingTitle(any()))
            .thenReturn(Single.just(true))
            .thenReturn(Single.just(false))

        val result = uploadWorker.findUniqueFileName(fileName)
        val regex = Regex("no_extension \\(\\d{3}\\)")

        assert(result.matches(regex))
    }
}