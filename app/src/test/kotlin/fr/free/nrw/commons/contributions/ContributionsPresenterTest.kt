package fr.free.nrw.commons.contributions

import android.database.Cursor
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.repository.UploadRepository
import io.reactivex.Completable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

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
    }

    /**
     * Test fetch contribution with filename
     */
    @Test
    fun testGetContributionWithFileName() {
        contributionsPresenter.getContributionsWithTitle("ashish")
        verify(repository).getContributionWithFileName("ashish")
    }


}