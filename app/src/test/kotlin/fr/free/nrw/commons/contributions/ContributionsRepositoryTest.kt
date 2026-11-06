package fr.free.nrw.commons.contributions

import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Scheduler
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

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
    fun testSaveContribution() {
        val contributions = listOf(mock(Contribution::class.java))
        whenever(localDataSource.saveContributions(ArgumentMatchers.anyList()))
            .thenReturn(Single.just(listOf(1L)))
        val save =
            contributionsRepository.save(contributions).test().assertValueAt(0) {
                it.size == 1 && it.get(0) == 1L
            }
    }
}
