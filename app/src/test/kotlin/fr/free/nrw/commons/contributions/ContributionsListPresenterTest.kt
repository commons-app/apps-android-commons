package fr.free.nrw.commons.contributions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

/**
 * The unit test class for ContributionsListPresenterTest
 */
class ContributionsListPresenterTest {
    @Mock
    internal lateinit var contributionBoundaryCallback: ContributionBoundaryCallback

    @Mock
    internal lateinit var repository: ContributionsRepository

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var scheduler: Scheduler

    lateinit var contributionsListPresenter: ContributionsListPresenter

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        scheduler = Schedulers.trampoline()
        contributionsListPresenter =
            ContributionsListPresenter(contributionBoundaryCallback, repository, scheduler);
    }

    @Test
    fun testDeleteUpload() {
        contributionsListPresenter.deleteUpload(mock(Contribution::class.java))
        verify(repository, times(1))
            .deleteContributionFromDB(ArgumentMatchers.any(Contribution::class.java));
    }
}