package fr.free.nrw.commons.media

import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.utils.CommonsDateUtil
import io.reactivex.Observable
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.wikipedia.dataclient.mwapi.ImageDetails
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.gallery.ImageInfo
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import java.util.*
import org.mockito.Captor
import org.mockito.Mockito.*


class MediaClientTest {

    @Mock
    internal var mediaInterface: MediaInterface? = null

    @InjectMocks
    var mediaClient: MediaClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun checkPageExistsUsingTitle() {
        val mwQueryPage = mock(MwQueryPage::class.java)
        `when`(mwQueryPage.pageId()).thenReturn(10)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        val checkPageExistsUsingTitle =
            mediaClient!!.checkPageExistsUsingTitle("File:Test.jpg").blockingGet()
        assertTrue(checkPageExistsUsingTitle)
    }

    @Test
    fun checkPageNotExistsUsingTitle() {
        val mwQueryPage = mock(MwQueryPage::class.java)
        `when`(mwQueryPage.pageId()).thenReturn(0)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf())
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        val checkPageExistsUsingTitle =
            mediaClient!!.checkPageExistsUsingTitle("File:Test.jpg").blockingGet()
        assertFalse(checkPageExistsUsingTitle)
    }

    @Test
    fun checkFileExistsUsingSha() {
        val mwQueryPage = mock(MwQueryPage::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.allImages()).thenReturn(listOf(mock(ImageDetails::class.java)))
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkFileExistsUsingSha(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        val checkFileExistsUsingSha = mediaClient!!.checkFileExistsUsingSha("abcde").blockingGet()
        assertTrue(checkFileExistsUsingSha)
    }

    @Test
    fun checkFileNotExistsUsingSha() {
        val mwQueryPage = mock(MwQueryPage::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.allImages()).thenReturn(listOf())
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkFileExistsUsingSha(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        val checkFileExistsUsingSha = mediaClient!!.checkFileExistsUsingSha("abcde").blockingGet()
        assertFalse(checkFileExistsUsingSha)
    }

    @Test
    fun getMedia() {
        val imageInfo = ImageInfo()

        val mwQueryPage = mock(MwQueryPage::class.java)
        `when`(mwQueryPage.title()).thenReturn("Test")
        `when`(mwQueryPage.imageInfo()).thenReturn(imageInfo)

        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.getMedia(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        assertEquals("Test", mediaClient!!.getMedia("abcde").blockingGet().filename)
    }

    @Test
    fun getMediaNull() {
        val imageInfo = ImageInfo()

        val mwQueryPage = mock(MwQueryPage::class.java)
        `when`(mwQueryPage.title()).thenReturn("Test")
        `when`(mwQueryPage.imageInfo()).thenReturn(imageInfo)

        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(null)
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.getMedia(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        assertEquals(Media.EMPTY, mediaClient!!.getMedia("abcde").blockingGet())
    }

    @Captor
    private val filenameCaptor: ArgumentCaptor<String>? = null

    @Test
    fun getPictureOfTheDay() {
        val template = "Template:Potd/" + CommonsDateUtil.getIso8601DateFormatShort().format(Date())

        val imageInfo = ImageInfo()

        val mwQueryPage = mock(MwQueryPage::class.java)
        `when`(mwQueryPage.title()).thenReturn("Test")
        `when`(mwQueryPage.imageInfo()).thenReturn(imageInfo)

        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.getMediaWithGenerator(filenameCaptor!!.capture()))
            .thenReturn(Observable.just(mockResponse))

        assertEquals("Test", mediaClient!!.getPictureOfTheDay().blockingGet().filename)
        assertEquals(template, filenameCaptor.value);
    }

    @Captor
    private val continuationCaptor: ArgumentCaptor<Map<String, String>>? = null

    @Test
    fun getMediaListFromCategoryTwice() {
        val mockContinuation = mapOf(Pair("gcmcontinue", "test"))
        val imageInfo = ImageInfo()

        val mwQueryPage = mock(MwQueryPage::class.java)
        `when`(mwQueryPage.title()).thenReturn("Test")
        `when`(mwQueryPage.imageInfo()).thenReturn(imageInfo)

        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))

        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)
        `when`(mockResponse.continuation()).thenReturn(mockContinuation)

        `when`(
            mediaInterface!!.getMediaListFromCategory(
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                continuationCaptor!!.capture()
            )
        )
            .thenReturn(Observable.just(mockResponse))
        val media1 = mediaClient!!.getMediaListFromCategory("abcde").blockingGet().get(0)
        val media2 = mediaClient!!.getMediaListFromCategory("abcde").blockingGet().get(0)

        assertEquals(continuationCaptor.allValues[0], emptyMap<String, String>())
        assertEquals(continuationCaptor.allValues[1], mockContinuation)

        assertEquals(media1.filename, "Test")
        assertEquals(media2.filename, "Test")
    }

    @Test
    fun getMediaListForUser() {
        val mockContinuation = mapOf("gcmcontinue" to "test")
        val imageInfo = ImageInfo()

        val mwQueryPage = mock(MwQueryPage::class.java)
        whenever(mwQueryPage.title()).thenReturn("Test")
        whenever(mwQueryPage.imageInfo()).thenReturn(imageInfo)

        val mwQueryResult = mock(MwQueryResult::class.java)
        whenever(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))

        val mockResponse = mock(MwQueryResponse::class.java)
        whenever(mockResponse.query()).thenReturn(mwQueryResult)
        whenever(mockResponse.continuation()).thenReturn(mockContinuation)

        whenever(
            mediaInterface!!.getMediaListForUser(
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                continuationCaptor!!.capture()
            )
        )
            .thenReturn(Observable.just(mockResponse))
        val media1 = mediaClient!!.getMediaListForUser("Test").blockingGet().get(0)
        val media2 = mediaClient!!.getMediaListForUser("Test").blockingGet().get(0)

        verify(mediaInterface, times(2))?.getMediaListForUser(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyInt(), ArgumentMatchers.anyMap<String, String>()
        )
    }

    @Test
    fun getPageHtmlTest() {
        val mwParseResult = mock(MwParseResult::class.java)

        `when`(mwParseResult.text()).thenReturn("Test")

        val mockResponse = MwParseResponse()
        mockResponse.setParse(mwParseResult)

        `when`(mediaInterface!!.getPageHtml(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        assertEquals("Test", mediaClient!!.getPageHtml("abcde").blockingGet())
    }

    @Test
    fun getPageHtmlTestNull() {
        val mockResponse = MwParseResponse()
        mockResponse.setParse(null)

        `when`(mediaInterface!!.getPageHtml(ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(mockResponse))

        assertEquals("", mediaClient!!.getPageHtml("abcde").blockingGet())
    }
}