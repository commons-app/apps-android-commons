import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.upload.worker.UploadWorker
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File

class UploadWorkerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

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
        // mockking: first check exists, second check unique
        whenever(mediaClient.checkPageExistsUsingTitle(any()))
            .thenReturn(Single.just(true))
            .thenReturn(Single.just(false))

        val result = uploadWorker.findUniqueFileName(fileName)

        // herre, updated to match the exactly 5 digits inside parentheses
        val regex = Regex("test_image \\(\\d{5}\\)\\.jpg")
        assert(result.matches(regex))
    }

    @Test
    fun `findUniqueFileName handles files without extensions`() {
        val fileName = "no_extension"
        whenever(mediaClient.checkPageExistsUsingTitle(any()))
            .thenReturn(Single.just(true))
            .thenReturn(Single.just(false))

        val result = uploadWorker.findUniqueFileName(fileName)
        val regex = Regex("no_extension \\(\\d{5}\\)")

        assert(result.matches(regex))
    }

    @Test
    fun `deleteUnusedUploadCopies deletes only old copies that no upload uses`() {
        val copies = temporaryFolder.newFolder()
        val twoDaysAgo = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
        val oldUnused = File(copies, "old-unused.jpg").apply { writeBytes(ByteArray(1)) }
        oldUnused.setLastModified(twoDaysAgo)
        val oldInUse = File(copies, "old-in-use.jpg").apply { writeBytes(ByteArray(1)) }
        oldInUse.setLastModified(twoDaysAgo)
        val newUnused = File(copies, "new-unused.jpg").apply { writeBytes(ByteArray(1)) }

        val contributionDao = mock<ContributionDao>()
        val fileUtilsWrapper = mock<FileUtilsWrapper>()
        val pending = mock<Contribution>()
        whenever(pending.localUriPath).thenReturn(oldInUse)
        whenever(contributionDao.getContribution(any<List<Int>>())).thenReturn(Single.just(listOf(pending)))
        whenever(uploadWorker.contributionDao).thenReturn(contributionDao)
        whenever(uploadWorker.fileUtilsWrapper).thenReturn(fileUtilsWrapper)
        whenever(uploadWorker.deleteUnusedUploadCopies(any())).thenCallRealMethod()

        uploadWorker.deleteUnusedUploadCopies(copies)

        verify(fileUtilsWrapper).deleteUploadFile(oldUnused)
        verify(fileUtilsWrapper, never()).deleteUploadFile(oldInUse)
        verify(fileUtilsWrapper, never()).deleteUploadFile(newUnused)
    }
}
