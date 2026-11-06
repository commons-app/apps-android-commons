package fr.free.nrw.commons.explore.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.media.MediaCollectionClient
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableMediaDataSourceTest {
    @Mock
    lateinit var mediaConverter: MediaConverter

    @Mock
    lateinit var mediaCollectionClient: MediaCollectionClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadFunction invokes mediaCollectionClient and has Label`() {
        whenever(mediaCollectionClient.getMediaListFromSearch("test", 0, 1))
            .thenReturn(Single.just(emptyList()))
        val pageableMediaDataSource = PageableMediaDataSource(mock(), mediaCollectionClient)
        pageableMediaDataSource.onQueryUpdated("test")
        Assert.assertEquals(pageableMediaDataSource.loadFunction(0, 1), emptyList<String>())
    }
}
