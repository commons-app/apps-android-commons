package fr.free.nrw.commons.explore.depictions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SearchDepictionsFragmentPresenterTest {

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    internal lateinit var view: SearchDepictionsFragmentContract.View

    private lateinit var searchDepictionsFragmentPresenter: SearchDepictionsFragmentPresenter

    private lateinit var testScheduler: TestScheduler

    @Mock
    private lateinit var searchableDepictionsDataSourceFactory: SearchableDepictionsDataSourceFactory

    private var loadingStates: PublishProcessor<LoadingState> = PublishProcessor.create()

    private var searchResults: PublishProcessor<LiveData<PagedList<DepictedItem>>> =
        PublishProcessor.create()

    private var noItemLoadedEvent: PublishProcessor<Unit> = PublishProcessor.create()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(searchableDepictionsDataSourceFactory.searchResults).thenReturn(searchResults)
        whenever(searchableDepictionsDataSourceFactory.loadingStates).thenReturn(loadingStates)
        whenever(searchableDepictionsDataSourceFactory.noItemsLoadedEvent)
            .thenReturn(noItemLoadedEvent)
        testScheduler = TestScheduler()
        searchDepictionsFragmentPresenter = SearchDepictionsFragmentPresenter(
            testScheduler,
            searchableDepictionsDataSourceFactory
        )
        searchDepictionsFragmentPresenter.onAttachView(view)
    }

    @Test
    fun `searchResults emission updates the view`() {
        val pagedListLiveData = mock<LiveData<PagedList<DepictedItem>>>()
        searchResults.offer(pagedListLiveData)
        verify(view).observeSearchResults(pagedListLiveData)
    }

    @Test
    fun `Loading offers a loading list item`() {
        onLoadingState(LoadingState.Loading)
        searchDepictionsFragmentPresenter.listFooterData.test()
            .assertValue(listOf(FooterItem.LoadingItem))
    }

    @Test
    fun `Complete offers an empty list item and hides initial loader`() {
        onLoadingState(LoadingState.Complete)
        searchDepictionsFragmentPresenter.listFooterData.test()
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
        searchDepictionsFragmentPresenter.onQueryUpdated("test")
        onLoadingState(LoadingState.Error)
        verify(view).setEmptyViewText("test")
        verify(view).showSnackbar()
        verify(view).hideInitialLoadProgress()
        searchDepictionsFragmentPresenter.listFooterData.test()
            .assertValue(listOf(FooterItem.RefreshItem))
    }

    @Test
    fun `Error offers a refresh list item, hides initial loader and shows error with a unset text`() {
        onLoadingState(LoadingState.Error)
        verify(view, never()).setEmptyViewText(any())
        verify(view).showSnackbar()
        verify(view).hideInitialLoadProgress()
        searchDepictionsFragmentPresenter.listFooterData.test()
            .assertValue(listOf(FooterItem.RefreshItem))
    }

    @Test
    fun `no Items event sets empty view text`() {
        searchDepictionsFragmentPresenter.onQueryUpdated("test")
        noItemLoadedEvent.offer(Unit)
        verify(view).setEmptyViewText("test")
    }

    @Test
    fun `retryFailedRequest calls retry`() {
        searchDepictionsFragmentPresenter.retryFailedRequest()
        verify(searchableDepictionsDataSourceFactory).retryFailedRequest()
    }

    @Test
    fun `onDetachView stops subscriptions`() {
        searchDepictionsFragmentPresenter.onDetachView()
        onLoadingState(LoadingState.Loading)
        searchDepictionsFragmentPresenter.listFooterData.test()
            .assertValue(emptyList())
    }

    @Test
    fun `onQueryUpdated updates dataSourceFactory`() {
        searchDepictionsFragmentPresenter.onQueryUpdated("test")
        verify(searchableDepictionsDataSourceFactory).onQueryUpdated("test")
    }

    private fun onLoadingState(loadingState: LoadingState) {
        loadingStates.offer(loadingState)
        testScheduler.triggerActions()
    }
}
