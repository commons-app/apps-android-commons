package fr.free.nrw.commons.upload

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_FAILED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_QUEUED
import fr.free.nrw.commons.contributions.ContributionsRepository
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.worker.WorkRequestHelper
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
import org.junit.Before
import org.junit.Test

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

        failedContributions.forEach { assertEquals(STATE_QUEUED, it.state) }
        verify(exactly = 40) { contributionsRepository.save(any<Contribution>()) }
    }

    /**
     * Test that retrying one failed upload queues that upload
     */
    @Test
    fun testRestartUploadQueuesTheGivenUpload() {
        val contribution = failedContribution()

        presenter.restartUpload(contribution, context)

        assertEquals(STATE_QUEUED, contribution.state)
        verify(exactly = 1) { contributionsRepository.save(contribution) }
    }

    private fun failedContribution() =
        Contribution(
            media = Media(),
            state = STATE_FAILED,
            localUri = mockk<Uri>(),
            contentUri = mockk<Uri>(),
        )
}
