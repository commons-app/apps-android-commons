package fr.free.nrw.commons.explore

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.explore.paging.LoadingState
import fr.free.nrw.commons.explore.paging.PagingDataSource
import fr.free.nrw.commons.explore.paging.PagingDataSourceFactory
import io.reactivex.processors.PublishProcessor
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class PagingDataSourceFactoryTest {

    @Mock
    private lateinit var depictsClient: DepictsClient

    @Mock
    private lateinit var loadingStates: PublishProcessor<LoadingState>
    private lateinit var factory: PagingDataSourceFactory<String>

    private var function: (Int, Int) -> List<String> = mock()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        factory = object : PagingDataSourceFactory<String>(loadingStates) {
            override val loadFunction get() = function
        }
    }

    @Test
    fun `create returns a dataSource`() {
        MatcherAssert.assertThat(
            factory.create(),
            instanceOf(PagingDataSource::class.java)
        )
    }

    @Test
    @Ignore("Rewrite with Mockk constructor mocks")
    fun `retryFailedRequest invokes method if not null`() {
        val spyFactory = spy(factory)
        val dataSource = mock<PagingDataSource<String>>()
        Mockito.doReturn(dataSource).`when`(spyFactory).create()
        factory.retryFailedRequest()
        verify(dataSource).retryFailedRequest()
    }

    @Test
    fun `retryFailedRequest does not invoke method if null`() {
        factory.retryFailedRequest()
    }
}
