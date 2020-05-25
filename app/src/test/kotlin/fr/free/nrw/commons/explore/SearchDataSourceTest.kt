package fr.free.nrw.commons.explore

import androidx.paging.PositionalDataSource
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.explore.depictions.LoadingStates
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SearchDataSourceTest {

    private lateinit var loadingStates: PublishProcessor<LoadingState>
    private lateinit var searchDepictionsDataSource: TestSearchDataSource

    @Mock
    private lateinit var mockGetItems: MockGetItems

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        MockitoAnnotations.initMocks(this)
        loadingStates = PublishProcessor.create()
        searchDepictionsDataSource =
            TestSearchDataSource(
                loadingStates,
                mockGetItems
            )
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
    }

    @Test
    fun `loadInitial returns results and emits InitialLoad & Complete`() {
        val params = PositionalDataSource.LoadInitialParams(0, 1, 2, false)
        val callback = mock<PositionalDataSource.LoadInitialCallback<String>>()
        whenever(mockGetItems.getItems(1, 0)).thenReturn(emptyList())
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadInitial(params, callback)
        verify(callback).onResult(emptyList(), 0)
        testSubscriber.assertValues(LoadingState.InitialLoad, LoadingState.Complete)
    }

    @Test
    fun `loadInitial onError does not return results and emits InitialLoad & Error`() {
        val params = PositionalDataSource.LoadInitialParams(0, 1, 2, false)
        val callback = mock<PositionalDataSource.LoadInitialCallback<String>>()
        whenever(mockGetItems.getItems(1, 0)).thenThrow(RuntimeException())
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadInitial(params, callback)
        verify(callback, never()).onResult(any(), any())
        testSubscriber.assertValues(LoadingState.InitialLoad, LoadingState.Error)
    }

    @Test
    fun `loadRange returns results and emits Loading & Complete`() {
        val callback: PositionalDataSource.LoadRangeCallback<String> = mock()
        val params = PositionalDataSource.LoadRangeParams(0, 1)
        whenever(mockGetItems.getItems(params.loadSize, params.startPosition))
            .thenReturn(emptyList())
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadRange(params, callback)
        verify(callback).onResult(emptyList())
        testSubscriber.assertValues(LoadingState.Loading, LoadingState.Complete)
    }

    @Test
    fun `loadRange onError does not return results and emits Loading & Error`() {
        val callback: PositionalDataSource.LoadRangeCallback<String> = mock()
        val params = PositionalDataSource.LoadRangeParams(0, 1)
        whenever(mockGetItems.getItems(params.loadSize, params.startPosition))
            .thenThrow(RuntimeException())
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadRange(params, callback)
        verify(callback, never()).onResult(any())
        testSubscriber.assertValues(LoadingState.Loading, LoadingState.Error)
    }

    @Test
    fun `retryFailedRequest does nothing when null`() {
        searchDepictionsDataSource.retryFailedRequest()
        verifyNoMoreInteractions(mockGetItems)
    }

    @Test
    fun `retryFailedRequest retries last request`() {
        val callback: PositionalDataSource.LoadRangeCallback<String> = mock()
        val params = PositionalDataSource.LoadRangeParams(0, 1)
        whenever(mockGetItems.getItems(params.loadSize, params.startPosition))
            .thenThrow(RuntimeException()).thenReturn(emptyList())
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadRange(params, callback)
        verify(callback, never()).onResult(any())
        searchDepictionsDataSource.retryFailedRequest()
        verify(callback).onResult(emptyList())
        testSubscriber.assertValues(
            LoadingState.Loading,
            LoadingState.Error,
            LoadingState.Loading,
            LoadingState.Complete
        )
    }
}

class TestSearchDataSource(loadingStates: LoadingStates, val mockGetItems: MockGetItems) :
    SearchDataSource<String>(loadingStates) {
    override fun getItems(loadSize: Int, startPosition: Int): List<String> =
        mockGetItems.getItems(loadSize, startPosition)
}

interface MockGetItems {
    fun getItems(loadSize: Int, startPosition: Int): List<String>
}
