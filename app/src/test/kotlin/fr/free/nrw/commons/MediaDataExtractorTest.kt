package fr.free.nrw.commons

import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

/**
 * Test methods in media data extractor
 */
class MediaDataExtractorTest {

    @Mock
    internal var mediaClient: MediaClient? = null
    @InjectMocks
    var mediaDataExtractor: MediaDataExtractor? = null

    /**
     * Init mocks for test
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    /**
     * test method to fetch media details
     */
    @Test
    fun fetchMediaDetails() {
        `when`(mediaClient?.getMedia(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(mock(Media::class.java)))

        `when`(mediaClient?.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(true))

        `when`(mediaClient?.getPageHtml(ArgumentMatchers.anyString()))
                .thenReturn(Single.just("Test"))

        val fetchMediaDetails = mediaDataExtractor?.fetchMediaDetails("test.jpg")?.blockingGet()

        assertTrue(fetchMediaDetails is Media)
    }
}