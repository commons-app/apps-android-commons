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
    internal var repository: ContributionsRepository? = null
    @Mock
    internal var view: ContributionsContract.View? = null

    private var contributionsPresenter: ContributionsPresenter? = null

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
        contributionsPresenter?.onAttachView(view)
        liveData=MutableLiveData()
    }

    /**
     * Test fetch contributions
     */
    @Test
    fun testFetchContributions(){
        Mockito.`when`(repository?.getString(ArgumentMatchers.anyString())).thenReturn("10")
        Mockito.`when`(repository?.fetchContributions()).thenReturn(liveData)
        contributionsPresenter?.fetchContributions()
        verify(repository)?.getString(ArgumentMatchers.anyString())
        verify(repository)?.fetchContributions()
    }

    /**
     * Test presenter actions onDeleteContribution
     */
    @Test
    fun testDeleteContribution() {
        Mockito.`when`(repository?.deleteContributionFromDB(ArgumentMatchers.any(Contribution::class.java))).thenReturn(Single.just(1))
        contributionsPresenter?.deleteUpload(contribution)
        verify(repository)?.deleteContributionFromDB(contribution)
    }

    /**
     * Test fetch contribution with filename
     */
    @Test
    fun testGetContributionWithFileName(){
        contributionsPresenter?.getContributionsWithTitle("ashish")
        verify(repository)?.getContributionWithFileName("ashish")
    }



}