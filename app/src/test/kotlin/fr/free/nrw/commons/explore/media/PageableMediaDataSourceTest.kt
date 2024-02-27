package fr.free.nrw.commons.explore.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

class PageableMediaDataSourceTest {
    @Mock
    lateinit var mediaConverter: MediaConverter
    @Mock
    lateinit var mediaClient: MediaClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadFunction invokes mediaClient and has Label`() {
        whenever(mediaClient.getMediaListFromSearch("test", 0, 1))
            .thenReturn(Single.just(emptyList()))
        val pageableMediaDataSource = PageableMediaDataSource(mock(), mediaClient)
        pageableMediaDataSource.onQueryUpdated("test")
        assertThat(pageableMediaDataSource.loadFunction(0,1), equalTo( emptyList<String>()))
    }
}
