package fr.free.nrw.commons.explore.categories.media

import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableCategoriesMediaDataSourceTest {
    @Mock
    lateinit var mediaClient: MediaClient

    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `loadFunction calls reset at position 0`() {
        val dataSource =
            PageableCategoriesMediaDataSource(liveDataConverter, mediaClient)
        dataSource.onQueryUpdated("test")
        whenever(mediaClient.getMediaListFromCategory("test"))
            .thenReturn(Single.just(emptyList()))
        assertThat(dataSource.loadFunction(-1, 0), `is`(emptyList()))
        verify(mediaClient).resetCategoryContinuation("test")
    }

    @Test
    fun `loadFunction does not call reset at any other position`() {
        val dataSource =
            PageableCategoriesMediaDataSource(liveDataConverter, mediaClient)
        dataSource.onQueryUpdated("test")
        whenever(mediaClient.getMediaListFromCategory("test"))
            .thenReturn(Single.just(emptyList()))
        assertThat(dataSource.loadFunction(-1, 1), `is`(emptyList()))
        verify(mediaClient, never()).resetCategoryContinuation("test")
    }
}
