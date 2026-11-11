package fr.free.nrw.commons.upload

import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_FAILED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_QUEUED
import fr.free.nrw.commons.contributions.ContributionsRepository
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.worker.WorkRequestHelper
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_DUPLICATE
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * The unit test class for PendingUploadsPresenter
 */
class PendingUploadsPresenterTest {
    private val contributionsRepository: ContributionsRepository = mockk()
    private val uploadRepository: UploadRepository = mockk()
    private val context: Context = mockk(relaxed = true)
    private lateinit var presenter: PendingUploadsPresenter

    @Before
    fun setUp() {
        // Starting the upload worker needs a real WorkManager, which is not available here
        mockkObject(WorkRequestHelper)
        every { WorkRequestHelper.makeOneTimeWorkRequest(any(), any()) } just Runs
        every { uploadRepository.checkDuplicateImage(any(), any()) } returns Single.just(IMAGE_OK)
        every { contributionsRepository.save(any<Contribution>()) } returns Completable.complete()
        every { contributionsRepository.deleteContributionFromDB(any()) } returns Completable.complete()
        presenter =
            PendingUploadsPresenter(
                mockk(relaxed = true),
                mockk(relaxed = true),
                contributionsRepository,
                uploadRepository,
                Schedulers.trampoline(),
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test that retrying all failed uploads queues every failed upload stored in the database,
     * not only the ones loaded in the list on the screen
     */
    @Test
    fun testRestartFailedUploadsQueuesEveryFailedUpload() {
        val failedContributions = List(40) { failedContribution() }
        every {
            contributionsRepository.getAllContributionsWithStates(listOf(STATE_FAILED))
        } returns Single.just(failedContributions)

        presenter.restartFailedUploads(context)

        failedContributions.forEach {
            assertEquals(STATE_QUEUED, it.state)
            assertNull(it.errorInfo)
        }
        verify(exactly = 40) { contributionsRepository.save(any<Contribution>()) }
        verify(exactly = 1) {
            WorkRequestHelper.makeOneTimeWorkRequest(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    /**
     * Test that retrying one failed upload queues that upload
     */
    @Test
    fun testRestartUploadQueuesTheGivenUpload() {
        val contribution = failedContribution(errorInfo = "Unsuccessful response")

        presenter.restartUpload(contribution, context)

        assertEquals(STATE_QUEUED, contribution.state)
        assertNull(contribution.errorInfo)
        verify(exactly = 1) { contributionsRepository.save(contribution) }
        verify(exactly = 1) {
            WorkRequestHelper.makeOneTimeWorkRequest(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    /**
     * Test that retrying a failed upload still queues it when the duplicate check fails
     * (e.g., offline or expired URI access)
     */
    @Test
    fun testRestartUploadQueuesUploadWhenDuplicateCheckFails() {
        val contribution = failedContribution(errorInfo = "Unsuccessful response")
        every {
            uploadRepository.checkDuplicateImage(any(), any())
        } returns Single.error(IOException("Unsuccessful response"))

        presenter.restartUpload(contribution, context)

        assertEquals(STATE_QUEUED, contribution.state)
        assertNull(contribution.errorInfo)
        verify(exactly = 1) { contributionsRepository.save(contribution) }
        verify(exactly = 1) {
            WorkRequestHelper.makeOneTimeWorkRequest(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    /**
     * Test that retrying a failed upload with null URIs queues safely without throwing
     */
    @Test
    fun testRestartUploadHandlesNullUris() {
        val contribution = failedContribution(localUri = null, contentUri = null)

        presenter.restartUpload(contribution, context)

        assertEquals(STATE_QUEUED, contribution.state)
        verify(exactly = 1) { contributionsRepository.save(contribution) }
        verify(exactly = 1) {
            WorkRequestHelper.makeOneTimeWorkRequest(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    /**
     * Test that retrying all failed uploads still queues items whose duplicate check fails
     */
    @Test
    fun testRestartFailedUploadsQueuesUploadWhenDuplicateCheckFails() {
        val first = failedContribution(errorInfo = "Unsuccessful response")
        val second = failedContribution()
        every {
            contributionsRepository.getAllContributionsWithStates(listOf(STATE_FAILED))
        } returns Single.just(listOf(first, second))
        every {
            uploadRepository.checkDuplicateImage(any(), any())
        } returnsMany listOf(Single.error(SecurityException("Permission denied")), Single.just(IMAGE_OK))

        presenter.restartFailedUploads(context)

        assertEquals(STATE_QUEUED, first.state)
        assertEquals(STATE_QUEUED, second.state)
        verify(exactly = 2) { contributionsRepository.save(any<Contribution>()) }
        verify(exactly = 1) {
            WorkRequestHelper.makeOneTimeWorkRequest(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    /**
     * Test that a failed upload already present on Commons is removed instead of re-queued
     */
    @Test
    fun testRestartUploadDeletesConfirmedDuplicate() {
        val contribution = failedContribution()
        every { uploadRepository.checkDuplicateImage(any(), any()) } returns Single.just(IMAGE_DUPLICATE)

        presenter.restartUpload(contribution, context)

        verify(exactly = 1) { contributionsRepository.deleteContributionFromDB(contribution) }
        verify(exactly = 0) { contributionsRepository.save(any<Contribution>()) }
        verify(exactly = 0) { WorkRequestHelper.makeOneTimeWorkRequest(any(), any()) }
    }

    private fun failedContribution(
        errorInfo: String? = null,
        localUri: Uri? = mockk<Uri>(),
        contentUri: Uri? = mockk<Uri>(),
    ) = Contribution(
        media = Media(),
        state = STATE_FAILED,
        errorInfo = errorInfo,
        localUri = localUri,
        contentUri = contentUri,
    )
}
