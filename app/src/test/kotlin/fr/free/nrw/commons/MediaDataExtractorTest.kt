package fr.free.nrw.commons

import fr.free.nrw.commons.media.FileDeletedOnCommonsException
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.IOException

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

        // val fetchMediaDetails = mediaDataExtractor?.fetchMediaDetails("File:Test.jpg", null)

        // assertTrue(fetchMediaDetails is Media)
    }

    @Test
    fun getWikiText() {
        `when`(mediaClient?.getCurrentWikiText(ArgumentMatchers.anyString()))
            .thenReturn(Single.just("Test"))

        assertEquals("Test", mediaDataExtractor!!.getCurrentWikiText("File:Test.jpg").blockingGet())
    }

    @Test
    fun refreshReportsAFileTheMediaLoadAlreadyFoundMissing() {
        val media = Media(pageId = "1", filename = "File:Gone.jpg")
        `when`(mediaClient!!.getMediaById(PAGE_ID_PREFIX + "1"))
            .thenReturn(Single.error(IOException("offline")))
        `when`(mediaClient!!.getMediaSuppressingErrors("File:Gone.jpg"))
            .thenReturn(Single.error(FileDeletedOnCommonsException()))

        try {
            mediaDataExtractor!!.refresh(media).blockingGet()
            fail("A deleted file from the media load should surface")
        } catch (e: RuntimeException) {
            assertTrue(e.cause is FileDeletedOnCommonsException)
        }
    }
}
