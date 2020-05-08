package fr.free.nrw.commons.contributions

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.utils.NetworkUtilsTest
import fr.free.nrw.commons.utils.createMockDataSourceFactory
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import java.util.*

/**
 * The unit test class for ContributionsPresenter
 */
class ContributionsListPresenterTest {
    var context: Context = NetworkUtilsTest.getContext(true);

    @Mock
    internal lateinit var repository: ContributionsRepository

    @Mock
    internal lateinit var sessionManager: SessionManager

    @Mock
    internal lateinit var mediaClient: MediaClient

    @Mock
    internal lateinit var view: ContributionsListContract.View

    private lateinit var contributionsListPresenter: ContributionsListPresenter

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var scheduler: Scheduler

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        scheduler = TestScheduler()
        whenever(repository.fetchContributions())
            .thenReturn(createMockDataSourceFactory(listOf(mock(Contribution::class.java))))
        contributionsListPresenter = ContributionsListPresenter(
            contributionBoundaryCallback, repository, scheduler
        )
        contributionsListPresenter.onAttachView(view)
    }

    @Test
    fun testFetchContributions() {
        whenever(sessionManager.userName).thenReturn("Test")
        whenever(mediaClient.getMediaListForUser(anyString())).thenReturn(
            Single.just(Arrays.asList(mock(Media::class.java)))
        )
        contributionsListPresenter.fetchContributions(context)
        verify(repository, times(1)).save(anyList());
        verify(mediaClient, times(1)).getMediaListForUser(anyString());
    }
}