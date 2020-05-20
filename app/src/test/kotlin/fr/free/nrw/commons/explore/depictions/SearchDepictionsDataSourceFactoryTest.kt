package fr.free.nrw.commons.explore.depictions

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.processors.PublishProcessor
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SearchDepictionsDataSourceFactoryTest {

    @Mock
    private lateinit var depictsClient: DepictsClient

    @Mock
    private lateinit var loadingStates: PublishProcessor<LoadingState>
    private lateinit var factory: SearchDepictionsDataSourceFactory

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        factory = SearchDepictionsDataSourceFactory(depictsClient, "test", loadingStates)
    }

    @Test
    fun `create returns a dataSource`() {
        assertThat(
            factory.create(),
            `is`(SearchDepictionsDataSource(depictsClient, loadingStates, "test"))
        )
    }

    @Test
    @Ignore("Rewrite with Mockk constructor mocks")
    fun `retryFailedRequest invokes method if not null`() {
        val spyFactory = spy(factory)
        val dataSource = mock<SearchDepictionsDataSource>()
        Mockito.doReturn(dataSource).`when`(spyFactory).create()
        factory.retryFailedRequest()
        verify(dataSource).retryFailedRequest()
    }

    @Test
    fun `retryFailedRequest does not invoke method if null`() {
        factory.retryFailedRequest()
    }
}
