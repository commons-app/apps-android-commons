package fr.free.nrw.commons.explore

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.explore.depictions.LoadFunction
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableDataSourceTest {

    @Mock
    private lateinit var liveDataConverter: LiveDataConverter

    private lateinit var pageableDataSource: PageableDataSource<String>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        pageableDataSource = object: PageableDataSource<String>(liveDataConverter){
            override val loadFunction: LoadFunction<String>
                get() = mock()

        }
    }

    @Test
    fun `onQueryUpdated emits new liveData`() {
        val (_, liveData) = expectNewLiveData()
        pageableDataSource.searchResults.test()
            .also { pageableDataSource.onQueryUpdated("test") }
            .assertValue(liveData)
    }

    @Test
    fun `onQueryUpdated invokes livedatconverter with no items emitter`() {
        val (zeroItemsFuncCaptor, _) = expectNewLiveData()
        pageableDataSource.onQueryUpdated("test")
        pageableDataSource.noItemsLoadedQueries.test()
            .also { zeroItemsFuncCaptor.firstValue.invoke() }
            .assertValue("test")
    }

    /*
    * Just for coverage, no way to really assert this
    * */
    @Test
    fun `retryFailedRequest does nothing without a factory`() {
        pageableDataSource.retryFailedRequest()
    }

    @Test
    @Ignore("Rewrite with Mockk constructor mocks")
    fun `retryFailedRequest retries with a factory`() {
        val (_, _, dataSourceFactoryCaptor) = expectNewLiveData()
        pageableDataSource.onQueryUpdated("test")
        val dataSourceFactory = spy(dataSourceFactoryCaptor.firstValue)
        pageableDataSource.retryFailedRequest()
        verify(dataSourceFactory).retryFailedRequest()
    }

    private fun expectNewLiveData(): Triple<KArgumentCaptor<() -> Unit>, LiveData<PagedList<String>>, KArgumentCaptor<SearchDataSourceFactory<String>>> {
        val captor = argumentCaptor<() -> Unit>()
        val dataSourceFactoryCaptor = argumentCaptor<SearchDataSourceFactory<String>>()
        val liveData: LiveData<PagedList<String>> = mock()
        whenever(liveDataConverter.convert(dataSourceFactoryCaptor.capture(), captor.capture()))
            .thenReturn(liveData)
        return Triple(captor, liveData, dataSourceFactoryCaptor)
    }
}
