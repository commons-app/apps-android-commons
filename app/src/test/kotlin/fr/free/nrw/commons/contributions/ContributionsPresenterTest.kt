package fr.free.nrw.commons.contributions

import android.database.Cursor
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Scheduler
import io.reactivex.Single
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
    internal lateinit var view: ContributionsContract.View

    private lateinit var contributionsPresenter: ContributionsPresenter

    private lateinit var cursor: Cursor

    lateinit var contribution: Contribution

    lateinit var loader: Loader<Cursor>

    lateinit var liveData: LiveData<List<Contribution>>

    @Rule @JvmField var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var scheduler : Scheduler

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        scheduler=TestScheduler()
        cursor = Mockito.mock(Cursor::class.java)
        contribution = Mockito.mock(Contribution::class.java)
        contributionsPresenter = ContributionsPresenter(repository,scheduler,scheduler)
        loader = Mockito.mock(CursorLoader::class.java)
        contributionsPresenter.onAttachView(view)
        liveData=MutableLiveData()
    }

    /**
     * Test presenter actions onDeleteContribution
     */
    @Test
    fun testDeleteContribution() {
        whenever(repository.deleteContributionFromDB(ArgumentMatchers.any(Contribution::class.java))).thenReturn(Single.just(1))
        contributionsPresenter.deleteUpload(contribution)
        verify(repository).deleteContributionFromDB(contribution)
    }

    /**
     * Test fetch contribution with filename
     */
    @Test
    fun testGetContributionWithFileName(){
        contributionsPresenter.getContributionsWithTitle("ashish")
        verify(repository).getContributionWithFileName("ashish")
    }



}