package fr.free.nrw.commons.explore.depictions

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.processors.PublishProcessor
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SearchableDepictionsDataSourceFactoryTest {

    @Mock
    private lateinit var searchDepictionsDataSourceFactoryFactory: SearchDepictionsDataSourceFactoryFactory

    @Mock
    private lateinit var liveDataConverter: LiveDataConverter

    private lateinit var factory: SearchableDepictionsDataSourceFactory


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        factory = SearchableDepictionsDataSourceFactory(
            searchDepictionsDataSourceFactoryFactory,
            liveDataConverter
        )
    }

    @Test
    fun `onQueryUpdated emits new liveData`() {
        val (_, liveData) = expectNewLiveData()
        factory.searchResults.test()
            .also { factory.onQueryUpdated("test") }
            .assertValue(liveData)
    }

    @Test
    fun `onQueryUpdated invokes livedatconverter with no items emitter`() {
        val (captor, _) = expectNewLiveData()
        factory.onQueryUpdated("test")
        factory.noItemsLoadedEvent.test()
            .also { captor.firstValue.invoke() }
            .assertValue(Unit)
    }

    /*
    * Just for coverage, no way to really assert this
    * */
    @Test
    fun `retryFailedRequest does nothing without a factory`() {
        factory.retryFailedRequest()
    }

    @Test
    fun `retryFailedRequest retries with a factory`() {
        val (_, _, dataSourceFactory) = expectNewLiveData()
        factory.onQueryUpdated("test")
        factory.retryFailedRequest()
        verify(dataSourceFactory).retryFailedRequest()
    }

    private fun expectNewLiveData(): Triple<KArgumentCaptor<() -> Unit>, LiveData<PagedList<DepictedItem>>, SearchDepictionsDataSourceFactory> {
        val dataSourceFactory: SearchDepictionsDataSourceFactory = mock()
        whenever(
            searchDepictionsDataSourceFactoryFactory.create(
                "test",
                factory.loadingStates as PublishProcessor<LoadingState>
            )
        ).thenReturn(dataSourceFactory)
        val captor = argumentCaptor<() -> Unit>()
        val liveData: LiveData<PagedList<DepictedItem>> = mock()
        whenever(liveDataConverter.convert(eq(dataSourceFactory), captor.capture()))
            .thenReturn(liveData)
        return Triple(captor, liveData, dataSourceFactory)
    }
}
