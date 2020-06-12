package fr.free.nrw.commons.explore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.*
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class BaseSearchPresenterTest {

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    internal lateinit var view: SearchFragmentContract.View<String>

    private lateinit var baseSearchPresenter: BaseSearchPresenter<String>

    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var pageableDataSource: PageableDataSource<String>

    private var loadingStates: PublishProcessor<LoadingState> = PublishProcessor.create()

    private var searchResults: PublishProcessor<LiveData<PagedList<String>>> =
        PublishProcessor.create()

    private var noItemLoadedEvent: PublishProcessor<Unit> = PublishProcessor.create()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(pageableDataSource.searchResults).thenReturn(searchResults)
        whenever(pageableDataSource.loadingStates).thenReturn(loadingStates)
        whenever(pageableDataSource.noItemsLoadedEvent)
            .thenReturn(noItemLoadedEvent)
        testScheduler = TestScheduler()
        baseSearchPresenter =
            object : BaseSearchPresenter<String>(testScheduler, pageableDataSource) {}
        baseSearchPresenter.onAttachView(view)
    }

    @Test
    fun `searchResults emission updates the view`() {
        val pagedListLiveData = mock<LiveData<PagedList<String>>>()
        searchResults.offer(pagedListLiveData)
        verify(view).observeSearchResults(pagedListLiveData)
    }

    @Test
    fun `Loading offers a loading list item`() {
        onLoadingState(LoadingState.Loading)
        baseSearchPresenter.listFooterData.test().assertValue(listOf(FooterItem.LoadingItem))
    }

    @Test
    fun `Complete offers an empty list item and hides initial loader`() {
        onLoadingState(LoadingState.Complete)
        baseSearchPresenter.listFooterData.test()
            .assertValue(emptyList())
        verify(view).hideInitialLoadProgress()
    }

    @Test
    fun `InitialLoad shows initial loader`() {
        onLoadingState(LoadingState.InitialLoad)
        verify(view).showInitialLoadInProgress()
    }

    @Test
    fun `Error offers a refresh list item, hides initial loader and shows error with a set text`() {
        baseSearchPresenter.onQueryUpdated("test")
        onLoadingState(LoadingState.Error)
        verify(view).setEmptyViewText("test")
        verify(view).showSnackbar()
        verify(view).hideInitialLoadProgress()
        baseSearchPresenter.listFooterData.test().assertValue(listOf(FooterItem.RefreshItem))
    }

    @Test
    fun `Error offers a refresh list item, hides initial loader and shows error with a unset text`() {
        onLoadingState(LoadingState.Error)
        verify(view, never()).setEmptyViewText(any())
        verify(view).showSnackbar()
        verify(view).hideInitialLoadProgress()
        baseSearchPresenter.listFooterData.test().assertValue(listOf(FooterItem.RefreshItem))
    }

    @Test
    fun `no Items event sets empty view text`() {
        baseSearchPresenter.onQueryUpdated("test")
        noItemLoadedEvent.offer(Unit)
        verify(view).setEmptyViewText("test")
    }

    @Test
    fun `retryFailedRequest calls retry`() {
        baseSearchPresenter.retryFailedRequest()
        verify(pageableDataSource).retryFailedRequest()
    }

    @Test
    fun `onDetachView stops subscriptions`() {
        baseSearchPresenter.onDetachView()
        onLoadingState(LoadingState.Loading)
        baseSearchPresenter.listFooterData.test().assertValue(emptyList())
    }

    @Test
    fun `onQueryUpdated updates dataSourceFactory`() {
        baseSearchPresenter.onQueryUpdated("test")
        verify(pageableDataSource).onQueryUpdated("test")
    }

    private fun onLoadingState(loadingState: LoadingState) {
        loadingStates.offer(loadingState)
        testScheduler.triggerActions()
    }
}
