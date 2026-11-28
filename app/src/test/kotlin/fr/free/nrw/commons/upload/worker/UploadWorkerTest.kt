import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.worker.UploadWorker
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

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
    fun `resumeInterruptedUploads puts uploads left in progress back in the queue`() {
        val contributionDao = mock<ContributionDao>()
        uploadWorker.contributionDao = contributionDao
        whenever(uploadWorker.contributionDao).thenReturn(contributionDao)
        whenever(uploadWorker.resumeInterruptedUploads()).thenCallRealMethod()
        val interrupted = Contribution(media = Media(), state = Contribution.STATE_IN_PROGRESS)
        whenever(contributionDao.getContribution(listOf(Contribution.STATE_IN_PROGRESS)))
            .thenReturn(Single.just(listOf(interrupted)))

        uploadWorker.resumeInterruptedUploads()

        assertEquals(Contribution.STATE_QUEUED, interrupted.state)
        verify(contributionDao).saveSynchronous(interrupted)
    }

    @Test
    fun `resumeInterruptedUploads leaves an upload this process is still sending`() {
        val contributionDao = mock<ContributionDao>()
        uploadWorker.contributionDao = contributionDao
        whenever(uploadWorker.contributionDao).thenReturn(contributionDao)
        whenever(uploadWorker.resumeInterruptedUploads()).thenCallRealMethod()
        val uploading = Contribution(media = Media(), state = Contribution.STATE_IN_PROGRESS)
        whenever(contributionDao.getContribution(listOf(Contribution.STATE_IN_PROGRESS)))
            .thenReturn(Single.just(listOf(uploading)))
        UploadWorker.uploadsInThisProcess.add(uploading.pageId)
        try {
            uploadWorker.resumeInterruptedUploads()
        } finally {
            UploadWorker.uploadsInThisProcess.remove(uploading.pageId)
        }

        assertEquals(Contribution.STATE_IN_PROGRESS, uploading.state)
        verify(contributionDao, never()).saveSynchronous(any())
    }

    @Test
    fun `resumeInterruptedUploads saves nothing when no upload is in progress`() {
        val contributionDao = mock<ContributionDao>()
        uploadWorker.contributionDao = contributionDao
        whenever(uploadWorker.contributionDao).thenReturn(contributionDao)
        whenever(uploadWorker.resumeInterruptedUploads()).thenCallRealMethod()
        whenever(contributionDao.getContribution(listOf(Contribution.STATE_IN_PROGRESS)))
            .thenReturn(Single.just(emptyList()))

        uploadWorker.resumeInterruptedUploads()

        verify(contributionDao, never()).saveSynchronous(any())
    }
}
