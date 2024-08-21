package fr.free.nrw.commons.explore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.explore.paging.*
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class BasePagingPresenterTest {

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    internal lateinit var view: PagingContract.View<String>

    private lateinit var basePagingPresenter: BasePagingPresenter<String>

    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var pageableBaseDataSource: PageableBaseDataSource<String>

    private var loadingStates: PublishProcessor<LoadingState> = PublishProcessor.create()

    private var searchResults: PublishProcessor<LiveData<PagedList<String>>> =
        PublishProcessor.create()

    private var noItemLoadedQueries: PublishProcessor<String> = PublishProcessor.create()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(pageableBaseDataSource.pagingResults).thenReturn(searchResults)
        whenever(pageableBaseDataSource.loadingStates).thenReturn(loadingStates)
        whenever(pageableBaseDataSource.noItemsLoadedQueries)
            .thenReturn(noItemLoadedQueries)
        testScheduler = TestScheduler()
        basePagingPresenter =
            object : BasePagingPresenter<String>(testScheduler, pageableBaseDataSource) {}
        basePagingPresenter.onAttachView(view)
    }

    @Test
    fun `searchResults emission updates the view`() {
        val pagedListLiveData = mock<LiveData<PagedList<String>>>()
        searchResults.offer(pagedListLiveData)
        verify(view).observePagingResults(pagedListLiveData)
    }

    @Test
    fun `Loading offers a loading list item`() {
        onLoadingState(LoadingState.Loading)
        verify(view).hideEmptyText()
        basePagingPresenter.listFooterData.test().assertValue(listOf(FooterItem.LoadingItem))
    }

    @Test
    fun `Complete offers an empty list item and hides initial loader`() {
        onLoadingState(LoadingState.Complete)
        basePagingPresenter.listFooterData.test()
            .assertValue(emptyList())
        verify(view).hideInitialLoadProgress()
    }

    @Test
    fun `InitialLoad shows initial loader`() {
        onLoadingState(LoadingState.InitialLoad)
        verify(view).hideEmptyText()
        verify(view).showInitialLoadInProgress()
    }

    @Test
    fun `Error offers a refresh list item, hides initial loader and shows error with a set text`() {
        basePagingPresenter.onQueryUpdated("test")
        onLoadingState(LoadingState.Error)
        verify(view).showSnackbar()
        verify(view).hideInitialLoadProgress()
        basePagingPresenter.listFooterData.test().assertValue(listOf(FooterItem.RefreshItem))
    }

    @Test
    fun `no Items event sets empty view text`() {
        noItemLoadedQueries.offer("test")
        verify(view).showEmptyText("test")
    }

    @Test
    fun `retryFailedRequest calls retry`() {
        basePagingPresenter.retryFailedRequest()
        verify(pageableBaseDataSource).retryFailedRequest()
    }

    @Test
    fun `onDetachView stops subscriptions`() {
        basePagingPresenter.onDetachView()
        onLoadingState(LoadingState.Loading)
        basePagingPresenter.listFooterData.test().assertValue(emptyList())
    }

    @Test
    fun `onQueryUpdated updates dataSourceFactory`() {
        basePagingPresenter.onQueryUpdated("test")
        verify(pageableBaseDataSource).onQueryUpdated("test")
    }

    private fun onLoadingState(loadingState: LoadingState) {
        loadingStates.offer(loadingState)
        testScheduler.triggerActions()
    }
}
