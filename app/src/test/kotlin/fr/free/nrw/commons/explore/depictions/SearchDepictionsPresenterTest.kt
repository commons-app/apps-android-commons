package fr.free.nrw.commons.explore.depictions

import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class SearchDepictionsPresenterTest {

    @Mock
    internal lateinit var view: SearchDepictionsFragmentContract.View

    private lateinit var searchDepictionsFragmentPresenter: SearchDepictionsFragmentPresenter

    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var jsonKvStore: JsonKvStore

    @Mock
    lateinit var recentSearchesDao: RecentSearchesDao

    @Mock
    lateinit var depictsClient: DepictsClient

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        searchDepictionsFragmentPresenter = SearchDepictionsFragmentPresenter(
            jsonKvStore,
            recentSearchesDao,
            depictsClient,
            testScheduler,
            testScheduler
        )
        searchDepictionsFragmentPresenter.onAttachView(view)
    }

    @Test
    fun updateDepictionList() {
        val expectedList = listOf(depictedItem())
        whenever(depictsClient.searchForDepictions("rabbit", 25, 0))
            .thenReturn(Single.just(expectedList))
        searchDepictionsFragmentPresenter.updateDepictionList("rabbit", 25, false)
        testScheduler.triggerActions()
        verify(view)?.onSuccess(expectedList)
    }
}
