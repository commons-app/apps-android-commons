package fr.free.nrw.commons.contributions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import media
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import java.lang.reflect.Method

/**
 * The unit test class for ContributionBoundaryCallbackTest
 */
class ContributionBoundaryCallbackTest {
    @Mock
    internal lateinit var repository: ContributionsRepository

    @Mock
    internal lateinit var sessionManager: SessionManager

    @Mock
    internal lateinit var mediaClient: MediaClient

    private lateinit var contributionBoundaryCallback: ContributionBoundaryCallback

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
        scheduler = Schedulers.trampoline()
        contributionBoundaryCallback =
            ContributionBoundaryCallback(repository, sessionManager, mediaClient, scheduler)
        contributionBoundaryCallback.userName = "test"
    }

    @Test
    fun testOnZeroItemsLoaded() {
        whenever(repository.save(anyList<Contribution>()))
            .thenReturn(Single.just(listOf(1L, 2L)))
        whenever(sessionManager.userName).thenReturn("Test")
        whenever(mediaClient.getMediaListForUser(anyString()))
            .thenReturn(Single.just(listOf(media())))
        contributionBoundaryCallback.onZeroItemsLoaded()
        verify(repository).save(anyList<Contribution>());
        verify(mediaClient).getMediaListForUser(anyString());
    }

    @Test
    fun testOnLastItemLoaded() {
        whenever(repository.save(anyList<Contribution>()))
            .thenReturn(Single.just(listOf(1L, 2L)))
        whenever(sessionManager.userName).thenReturn("Test")
        whenever(mediaClient.getMediaListForUser(anyString()))
            .thenReturn(Single.just(listOf(media())))
        contributionBoundaryCallback.onItemAtEndLoaded(mock(Contribution::class.java))
        verify(repository).save(anyList());
        verify(mediaClient).getMediaListForUser(anyString());
    }

    @Test
    fun testOnFrontItemLoaded() {
        whenever(repository.save(anyList<Contribution>()))
            .thenReturn(Single.just(listOf(1L, 2L)))
        whenever(sessionManager.userName).thenReturn("Test")
        whenever(mediaClient.getMediaListForUser(anyString()))
            .thenReturn(Single.just(listOf(media())))
        contributionBoundaryCallback.onItemAtFrontLoaded(mock(Contribution::class.java))
    }

    @Test
    fun testFetchContributions() {
        whenever(repository.save(anyList<Contribution>()))
            .thenReturn(Single.just(listOf(1L, 2L)))
        whenever(sessionManager.userName).thenReturn("Test")
        whenever(mediaClient.getMediaListForUser(anyString())).thenReturn(
            Single.just(listOf(media()))
        )
        val method: Method = ContributionBoundaryCallback::class.java.getDeclaredMethod(
            "fetchContributions"
        )
        method.isAccessible = true
        method.invoke(contributionBoundaryCallback)
        verify(repository).save(anyList());
        verify(mediaClient).getMediaListForUser(anyString());
    }

    @Test
    fun testFetchContributionsFailed() {
        whenever(sessionManager.userName).thenReturn("Test")
        whenever(mediaClient.getMediaListForUser(anyString())).thenReturn(Single.error(Exception("Error")))
        val method: Method = ContributionBoundaryCallback::class.java.getDeclaredMethod(
            "fetchContributions"
        )
        method.isAccessible = true
        method.invoke(contributionBoundaryCallback)
        verifyNoInteractions(repository)
        verify(mediaClient).getMediaListForUser(anyString());
    }
}
