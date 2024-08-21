package fr.free.nrw.commons.explore

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.explore.depictions.search.LoadFunction
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.explore.paging.PagingDataSourceFactory
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableBaseDataSourceTest {

    @Mock
    private lateinit var liveDataConverter: LiveDataConverter

    private lateinit var pageableBaseDataSource: PageableBaseDataSource<String>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        pageableBaseDataSource = object: PageableBaseDataSource<String>(liveDataConverter){
            override val loadFunction: LoadFunction<String>
                get() = mock()

        }
    }

    @Test
    fun `onQueryUpdated emits new liveData`() {
        val (_, liveData) = expectNewLiveData()
        pageableBaseDataSource.pagingResults.test()
            .also { pageableBaseDataSource.onQueryUpdated("test") }
            .assertValue(liveData)
    }

    @Test
    fun `onQueryUpdated invokes livedatconverter with no items emitter`() {
        val (zeroItemsFuncCaptor, _) = expectNewLiveData()
        pageableBaseDataSource.onQueryUpdated("test")
        pageableBaseDataSource.noItemsLoadedQueries.test()
            .also { zeroItemsFuncCaptor.firstValue.invoke() }
            .assertValue("test")
    }

    /*
    * Just for coverage, no way to really assert this
    * */
    @Test
    fun `retryFailedRequest does nothing without a factory`() {
        pageableBaseDataSource.retryFailedRequest()
    }

    @Test
    @Ignore("Rewrite with Mockk constructor mocks")
    fun `retryFailedRequest retries with a factory`() {
        val (_, _, dataSourceFactoryCaptor) = expectNewLiveData()
        pageableBaseDataSource.onQueryUpdated("test")
        val dataSourceFactory = spy(dataSourceFactoryCaptor.firstValue)
        pageableBaseDataSource.retryFailedRequest()
        verify(dataSourceFactory).retryFailedRequest()
    }

    private fun expectNewLiveData(): Triple<KArgumentCaptor<() -> Unit>, LiveData<PagedList<String>>, KArgumentCaptor<PagingDataSourceFactory<String>>> {
        val captor = argumentCaptor<() -> Unit>()
        val dataSourceFactoryCaptor = argumentCaptor<PagingDataSourceFactory<String>>()
        val liveData: LiveData<PagedList<String>> = mock()
        whenever(liveDataConverter.convert(dataSourceFactoryCaptor.capture(), captor.capture()))
            .thenReturn(liveData)
        return Triple(captor, liveData, dataSourceFactoryCaptor)
    }
}
