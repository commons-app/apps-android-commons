package fr.free.nrw.commons.explore.depictions

import androidx.paging.PositionalDataSource
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.explore.depictions.LoadingState.*
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SearchDepictionsDataSourceTest {

    @Mock
    private lateinit var depictsClient: DepictsClient

    private lateinit var loadingStates: PublishProcessor<LoadingState>
    private lateinit var searchDepictionsDataSource: SearchDepictionsDataSource

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        MockitoAnnotations.initMocks(this)
        loadingStates = PublishProcessor.create()
        searchDepictionsDataSource =
            SearchDepictionsDataSource(depictsClient, loadingStates, "test")
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
    }

    @Test
    fun `loadInitial returns results and emits InitialLoad & Complete`() {
        val params = PositionalDataSource.LoadInitialParams(0, 1, 2, false)
        val callback = mock<PositionalDataSource.LoadInitialCallback<DepictedItem>>()
        whenever(depictsClient.searchForDepictions("test", 1, 0))
            .thenReturn(Single.just(emptyList()))
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadInitial(params, callback)
        verify(callback).onResult(emptyList(), 0)
        testSubscriber.assertValues(InitialLoad, Complete)
    }

    @Test
    fun `loadInitial onError does not return results and emits InitialLoad & Error`() {
        val params = PositionalDataSource.LoadInitialParams(0, 1, 2, false)
        val callback = mock<PositionalDataSource.LoadInitialCallback<DepictedItem>>()
        whenever(depictsClient.searchForDepictions("test", 1, 0))
            .thenThrow(RuntimeException())
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadInitial(params, callback)
        verify(callback, never()).onResult(any(), any())
        testSubscriber.assertValues(InitialLoad, Error)
    }

    @Test
    fun `loadRange returns results and emits Loading & Complete`() {
        val callback: PositionalDataSource.LoadRangeCallback<DepictedItem> = mock()
        val params = PositionalDataSource.LoadRangeParams(0, 1)
        whenever(depictsClient.searchForDepictions("test", params.loadSize, params.startPosition))
            .thenReturn(Single.just(emptyList()))
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadRange(params, callback)
        verify(callback).onResult(emptyList())
        testSubscriber.assertValues(Loading, Complete)
    }

    @Test
    fun `loadRange onError does not return results and emits Loading & Error`() {
        val callback: PositionalDataSource.LoadRangeCallback<DepictedItem> = mock()
        val params = PositionalDataSource.LoadRangeParams(0, 1)
        whenever(depictsClient.searchForDepictions("test", params.loadSize, params.startPosition))
            .thenThrow(RuntimeException())
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadRange(params, callback)
        verify(callback, never()).onResult(any())
        testSubscriber.assertValues(Loading, Error)
    }

    @Test
    fun `retryFailedRequest does nothing when null`() {
        searchDepictionsDataSource.retryFailedRequest()
        verifyNoMoreInteractions(depictsClient)
    }

    @Test
    fun `retryFailedRequest retries last request`() {
        val callback: PositionalDataSource.LoadRangeCallback<DepictedItem> = mock()
        val params = PositionalDataSource.LoadRangeParams(0, 1)
        whenever(depictsClient.searchForDepictions("test", params.loadSize, params.startPosition))
            .thenThrow(RuntimeException()).thenReturn(Single.just(emptyList()))
        val testSubscriber = loadingStates.test()
        searchDepictionsDataSource.loadRange(params, callback)
        verify(callback, never()).onResult(any())
        searchDepictionsDataSource.retryFailedRequest()
        verify(callback).onResult(emptyList())
        testSubscriber.assertValues(Loading, Error, Loading, Complete)
    }
}
