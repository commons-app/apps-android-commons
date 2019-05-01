package fr.free.nrw.commons

import fr.free.nrw.commons.mwapi.MediaResult
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.Single
import junit.framework.Assert.assertTrue
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
    internal var mwApi: MediaWikiApi? = null

    @Mock
    internal var okHttpJsonApiClient: OkHttpJsonApiClient? = null

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
        `when`(okHttpJsonApiClient?.getMedia(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean()))
                .thenReturn(Single.just(mock(Media::class.java)))

        `when`(mwApi?.pageExists(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(true))

        val mediaResult = mock(MediaResult::class.java)
        `when`(mediaResult.wikiSource).thenReturn("some wiki source")
        `when`(mwApi?.fetchMediaByFilename(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(mediaResult))

        `when`(mwApi?.parseWikicode(ArgumentMatchers.anyString()))
                .thenReturn(Single.just("discussion text"))

        val fetchMediaDetails = mediaDataExtractor?.fetchMediaDetails("test.jpg")?.blockingGet()

        assertTrue(fetchMediaDetails is Media)
    }
}