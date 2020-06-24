package fr.free.nrw.commons.contributions

import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.utils.createMockDataSourceFactory
import io.reactivex.Scheduler
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.mock

/**
 * The unit test class for ContributionsRepositoryTest
 */
class ContributionsRepositoryTest {
    @Mock
    internal lateinit var localDataSource: ContributionsLocalDataSource

    @InjectMocks
    private lateinit var contributionsRepository: ContributionsRepository

    lateinit var scheduler: Scheduler

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testFetchContributions() {
        val contribution = mock(Contribution::class.java)
        whenever(localDataSource.getContributions())
            .thenReturn(createMockDataSourceFactory(listOf(contribution)))
        val contributionsFactory = contributionsRepository.fetchContributions()
        verify(localDataSource, times(1)).getContributions();
    }

    @Test
    fun testSaveContribution() {
        val contributions = listOf(mock(Contribution::class.java))
        whenever(localDataSource.saveContributions(ArgumentMatchers.anyList()))
            .thenReturn(Single.just(listOf(1L)))
        val save = contributionsRepository.save(contributions).test().assertValueAt(0) {
            it.size == 1 && it.get(0) == 1L
        }
    }
}