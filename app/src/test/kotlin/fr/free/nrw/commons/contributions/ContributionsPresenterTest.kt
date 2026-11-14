package fr.free.nrw.commons.contributions

import android.content.Context
import android.database.Cursor
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.work.ExistingWorkPolicy
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.worker.WorkRequestHelper
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_DUPLICATE
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.IOException
import io.mockk.verify as mockkVerify

/**
 * The unit test class for ContributionsPresenter
 */
class ContributionsPresenterTest {
    @Mock
    internal lateinit var repository: ContributionsRepository

    @Mock
    internal lateinit var uploadRepository: UploadRepository

    @Mock
    internal lateinit var view: ContributionsContract.View

    @Mock
    internal lateinit var context: Context

    private lateinit var contributionsPresenter: ContributionsPresenter

    private lateinit var cursor: Cursor

    lateinit var contribution: Contribution

    lateinit var loader: Loader<Cursor>

    lateinit var liveData: LiveData<List<Contribution>>

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var scheduler: TestScheduler

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        scheduler = TestScheduler()
        cursor = Mockito.mock(Cursor::class.java)
        contribution = Mockito.mock(Contribution::class.java)
        contributionsPresenter = ContributionsPresenter(repository, uploadRepository, scheduler)
        loader = Mockito.mock(CursorLoader::class.java)
        contributionsPresenter.onAttachView(view)
        liveData = MutableLiveData()
        whenever(context.applicationContext).thenReturn(context)
        whenever(view.getContext()).thenReturn(context)
        mockkObject(WorkRequestHelper)
        every { WorkRequestHelper.makeOneTimeWorkRequest(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test fetch contribution with filename
     */
    @Test
    fun testGetContributionWithFileName() {
        contributionsPresenter.getContributionsWithTitle("ashish")
        verify(repository).getContributionWithFileName("ashish")
    }

    @Test
    fun testCheckDuplicateImageAndRestartContributionQueuesOnOk() {
        val failed =
            Contribution(
                media = Media(),
                state = Contribution.STATE_FAILED,
                errorInfo = "Unsuccessful response",
            )
        whenever(uploadRepository.checkDuplicateImage(anyOrNull(), anyOrNull()))
            .thenReturn(Single.just(IMAGE_OK))
        whenever(repository.save(failed)).thenReturn(Completable.complete())

        contributionsPresenter.checkDuplicateImageAndRestartContribution(failed)
        scheduler.triggerActions()

        assertEquals(Contribution.STATE_QUEUED, failed.state)
        assertNull(failed.errorInfo)
        verify(repository).save(failed)
        mockkVerify(exactly = 1) {
            WorkRequestHelper.makeOneTimeWorkRequest(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    @Test
    fun testCheckDuplicateImageAndRestartContributionQueuesWhenDuplicateCheckErrors() {
        val failed =
            Contribution(
                media = Media(),
                state = Contribution.STATE_FAILED,
                errorInfo = "Unsuccessful response",
            )
        whenever(uploadRepository.checkDuplicateImage(anyOrNull(), anyOrNull()))
            .thenReturn(Single.error(IOException("Unsuccessful response")))
        whenever(repository.save(failed)).thenReturn(Completable.complete())

        contributionsPresenter.checkDuplicateImageAndRestartContribution(failed)
        scheduler.triggerActions()

        assertEquals(Contribution.STATE_QUEUED, failed.state)
        assertNull(failed.errorInfo)
        verify(repository).save(failed)
        mockkVerify(exactly = 1) {
            WorkRequestHelper.makeOneTimeWorkRequest(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    @Test
    fun testCheckDuplicateImageAndRestartContributionDeletesDuplicate() {
        val failed = Contribution(media = Media(), state = Contribution.STATE_FAILED)
        whenever(uploadRepository.checkDuplicateImage(anyOrNull(), anyOrNull()))
            .thenReturn(Single.just(IMAGE_DUPLICATE))
        whenever(repository.deleteContributionFromDB(failed)).thenReturn(Completable.complete())

        contributionsPresenter.checkDuplicateImageAndRestartContribution(failed)
        scheduler.triggerActions()

        verify(repository).deleteContributionFromDB(failed)
    }
}
