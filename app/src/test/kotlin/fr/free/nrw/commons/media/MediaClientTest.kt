package fr.free.nrw.commons.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.media.MediaConverter
import fr.free.nrw.commons.media.model.PageMediaListItem
import fr.free.nrw.commons.media.model.PageMediaListResponse
import fr.free.nrw.commons.utils.CommonsDateUtil
import io.reactivex.Single
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.*
import org.wikipedia.dataclient.mwapi.ImageDetails
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.wikidata.Entities
import java.util.*


class MediaClientTest {

    @Mock
    internal var mediaInterface: MediaInterface? = null
    @Mock
    internal var mediaConverter: MediaConverter? = null
    @Mock
    internal var mediaDetailInterface: MediaDetailInterface? = null

    @Mock
    internal var pageMediaInterface: PageMediaInterface? = null


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
            .thenReturn(Single.just(mockResponse))

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
            .thenReturn(Single.just(mockResponse))

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
            .thenReturn(Single.just(mockResponse))

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
            .thenReturn(Single.just(mockResponse))

        val checkFileExistsUsingSha = mediaClient!!.checkFileExistsUsingSha("abcde").blockingGet()
        assertFalse(checkFileExistsUsingSha)
    }

    @Test
    fun getMedia() {
        val (mockResponse, media: Media) = expectGetEntitiesAndMediaConversion()

        `when`(mediaInterface!!.getMedia(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(mockResponse))

        mediaClient!!.getMedia("abcde").test().assertValue(media)
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
            .thenReturn(Single.just(mockResponse))
        mediaClient!!.getMedia("abcde").test().assertErrorMessage("List is empty.")
    }

    @Test
    fun getPictureOfTheDay() {
        val template = "Template:Potd/" + CommonsDateUtil.getIso8601DateFormatShort().format(Date())

        val (mockResponse, media: Media) = expectGetEntitiesAndMediaConversion()
        `when`(mediaInterface!!.getMediaWithGenerator(template))
            .thenReturn(Single.just(mockResponse))
        mediaClient!!.getPictureOfTheDay().test().assertValue(media)
    }

    private fun expectGetEntitiesAndMediaConversion(): Pair<MwQueryResponse, Media> {
        val mockResponse = mock(MwQueryResponse::class.java)
        val queryResult: MwQueryResult = mock()
        whenever(mockResponse.query()).thenReturn(queryResult)
        val queryPage: MwQueryPage = mock()
        whenever(queryResult.pages()).thenReturn(listOf(queryPage))
        whenever(queryPage.pageId()).thenReturn(0)
        val entities: Entities = mock()
        whenever(mediaDetailInterface!!.getEntity("M0")).thenReturn(Single.just(entities))
        val entity: Entities.Entity = mock()
        whenever(entities.entities()).thenReturn(mapOf("id" to entity))
        val media: Media = mock()
        whenever(mediaConverter!!.convert(queryPage, entity)).thenReturn(media)
        return Pair(mockResponse, media)
    }

    @Captor
    private val continuationCaptor: ArgumentCaptor<Map<String, String>>? = null

    @Test
    fun getMediaListFromCategoryTwice() {
        val mockContinuation = mapOf(Pair("gcmcontinue", "test"))

        val (mockResponse, media: Media) = expectGetEntitiesAndMediaConversion()
        `when`(mockResponse.continuation()).thenReturn(mockContinuation)

        `when`(
            mediaInterface!!.getMediaListFromCategory(
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                continuationCaptor!!.capture()
            )
        )
            .thenReturn(Single.just(mockResponse))

        val media1 = mediaClient!!.getMediaListFromCategory("abcde").blockingGet().get(0)
        val media2 = mediaClient!!.getMediaListFromCategory("abcde").blockingGet().get(0)

        assertEquals(continuationCaptor.allValues[0], emptyMap<String, String>())
        assertEquals(continuationCaptor.allValues[1], mockContinuation)

        assertEquals(media1, media)
        assertEquals(media2, media)
    }

    @Test
    fun getMediaListForUser() {
        val mockContinuation = mapOf("gcmcontinue" to "test")

        val (mockResponse, media: Media) = expectGetEntitiesAndMediaConversion()
        whenever(mockResponse.continuation()).thenReturn(mockContinuation)

        whenever(
            mediaInterface!!.getMediaListForUser(
                ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(),
                continuationCaptor!!.capture()
            )
        )
            .thenReturn(Single.just(mockResponse))
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
            .thenReturn(Single.just(mockResponse))

        assertEquals("Test", mediaClient!!.getPageHtml("abcde").blockingGet())
    }

    @Test
    fun doesPageContainMedia() {
        val mock = mock(PageMediaListResponse::class.java)
        whenever(mock.items).thenReturn(listOf<PageMediaListItem>(mock(PageMediaListItem::class.java)))
        `when`(pageMediaInterface!!.getMediaList(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(mock))

        mediaClient!!.doesPageContainMedia("Test").test().assertValue(true)
    }

    @Test
    fun doesPageContainMediaWithNoMedia() {
        val mock = mock(PageMediaListResponse::class.java)
        whenever(mock.items).thenReturn(listOf<PageMediaListItem>())
        `when`(pageMediaInterface!!.getMediaList(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(mock))

        mediaClient!!.doesPageContainMedia("Test").test().assertValue(false)
    }

    @Test
    fun getPageHtmlTestNull() {
        val mockResponse = MwParseResponse()
        mockResponse.setParse(null)

        `when`(mediaInterface!!.getPageHtml(ArgumentMatchers.anyString()))
            .thenReturn(Single.just(mockResponse))

        assertEquals("", mediaClient!!.getPageHtml("abcde").blockingGet())
    }
}
