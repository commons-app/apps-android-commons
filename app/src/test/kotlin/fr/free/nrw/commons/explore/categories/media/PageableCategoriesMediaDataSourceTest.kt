package fr.free.nrw.commons.explore.categories.media

import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.media.MediaCollectionClient
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableCategoriesMediaDataSourceTest {
    @Mock
    lateinit var mediaCollectionClient: MediaCollectionClient

    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadFunction calls reset at position 0`() {
        val dataSource =
            PageableCategoriesMediaDataSource(liveDataConverter, mediaCollectionClient)
        dataSource.onQueryUpdated("test")
        whenever(mediaCollectionClient.getMediaListFromCategory("test"))
            .thenReturn(Single.just(emptyList()))
        Assert.assertEquals(dataSource.loadFunction(-1, 0), emptyList<String>())
        verify(mediaCollectionClient).resetCategoryContinuation("test")
    }

    @Test
    fun `loadFunction does not call reset at any other position`() {
        val dataSource =
            PageableCategoriesMediaDataSource(liveDataConverter, mediaCollectionClient)
        dataSource.onQueryUpdated("test")
        whenever(mediaCollectionClient.getMediaListFromCategory("test"))
            .thenReturn(Single.just(emptyList()))
        Assert.assertEquals(dataSource.loadFunction(-1, 1), emptyList<String>())
        verify(mediaCollectionClient, never()).resetCategoryContinuation("test")
    }
}
