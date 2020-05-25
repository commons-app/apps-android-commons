package fr.free.nrw.commons.explore

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.explore.depictions.DepictsClient
import io.reactivex.processors.PublishProcessor
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SearchDataSourceFactoryTest {

    @Mock
    private lateinit var depictsClient: DepictsClient

    @Mock
    private lateinit var loadingStates: PublishProcessor<LoadingState>
    private lateinit var factory: SearchDataSourceFactory<String>

    private var function: (Int, Int) -> List<String> = mock()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        factory = object : SearchDataSourceFactory<String>(loadingStates) {
            override val loadFunction get() = function
        }
    }

    @Test
    fun `create returns a dataSource`() {
        assertThat(
            factory.create(),
            instanceOf(SearchDataSource::class.java)
        )
    }

    @Test
    @Ignore("Rewrite with Mockk constructor mocks")
    fun `retryFailedRequest invokes method if not null`() {
        val spyFactory = spy(factory)
        val dataSource = mock<SearchDataSource<String>>()
        Mockito.doReturn(dataSource).`when`(spyFactory).create()
        factory.retryFailedRequest()
        verify(dataSource).retryFailedRequest()
    }

    @Test
    fun `retryFailedRequest does not invoke method if null`() {
        factory.retryFailedRequest()
    }
}
