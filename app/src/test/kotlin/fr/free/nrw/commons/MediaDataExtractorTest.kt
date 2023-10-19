package fr.free.nrw.commons

import com.nhaarman.mockitokotlin2.whenever
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
import org.wikipedia.dataclient.mwapi.MwQueryResponse

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
        MockitoAnnotations.openMocks(this)
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

        //val fetchMediaDetails = mediaDataExtractor?.fetchMediaDetails("File:Test.jpg", null)

        //assertTrue(fetchMediaDetails is Media)
    }

    @Test
    fun getWikiText() {
        `when`(mediaDataExtractor?.getCurrentWikiText(ArgumentMatchers.anyString()))
            .thenReturn(Single.just("Test"))
    }
}