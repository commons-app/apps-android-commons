package fr.free.nrw.commons.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.media.MediaConverter
import fr.free.nrw.commons.media.model.PageMediaListResponse
import io.reactivex.Single
import media
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.wikidata.Entities


class MediaClientTest {

    @Mock
    internal lateinit var mediaInterface: MediaInterface

    @Mock
    internal lateinit var mediaConverter: MediaConverter

    @Mock
    internal lateinit var mediaDetailInterface: MediaDetailInterface

    @Mock
    internal lateinit var pageMediaInterface: PageMediaInterface

    val continuationMap = mapOf("continuation" to "continuation")

    private lateinit var mediaClient: MediaClient

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mediaClient =
            MediaClient(mediaInterface, pageMediaInterface, mediaDetailInterface, mediaConverter)
    }

    @Test
    fun `getMediaById maps response of interface`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping()
        whenever(mediaInterface.getMediaById("id")).thenReturn(Single.just(mwQueryResponse))
        mediaClient.getMediaById("id").test().assertValue(media)
    }

    @Test
    fun `checkPageExistsUsingTitle returns true for greater than 0 id`() {
        val mwQueryResponse = expectResponseWithPageId(1)
        whenever(mediaInterface.checkPageExistsUsingTitle(""))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.checkPageExistsUsingTitle("").test().assertValue(true)
    }

    @Test
    fun `checkPageExistsUsingTitle returns false for 0 id`() {
        val mwQueryResponse = expectResponseWithPageId(0)
        whenever(mediaInterface.checkPageExistsUsingTitle(""))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.checkPageExistsUsingTitle("").test().assertValue(false)
    }

    @Test
    fun `checkFileExistsUsingSha returns false with no Images`() {
        val mwQueryResponse = mockQuery {
            whenever(allImages()).thenReturn(listOf())
        }
        whenever(mediaInterface.checkFileExistsUsingSha(""))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.checkFileExistsUsingSha("").test().assertValue(false)
    }

    @Test
    fun `checkFileExistsUsingSha returns true with Images`() {
        val mwQueryResponse = mockQuery {
            whenever(allImages()).thenReturn(listOf(mock()))
        }
        whenever(mediaInterface.checkFileExistsUsingSha(""))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.checkFileExistsUsingSha("").test().assertValue(true)
    }

    @Test
    fun `getMediaListFromCategory is continuable and returns mapped response`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping(continuationMap)
        whenever(mediaInterface.getMediaListFromCategory("", 10, emptyMap()))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.getMediaListFromCategory("").test().assertValues(listOf(media))

        whenever(mediaInterface.getMediaListFromCategory("", 10, continuationMap))
            .thenReturn(Single.error(Exception()))
        mediaClient.getMediaListFromCategory("").test().assertError { true }

        mediaClient.resetCategoryContinuation("")
        val (resetMwQueryResponse, resetMedia)=expectSuccessfulMapping()
        whenever(mediaInterface.getMediaListFromCategory("", 10, emptyMap()))
            .thenReturn(Single.just(resetMwQueryResponse))
        mediaClient.getMediaListFromCategory("").test().assertValues(listOf(resetMedia))
    }

    @Test
    fun `getMediaListForUser is continuable and returns mapped response`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping(null)
        whenever(mediaInterface.getMediaListForUser("", 10, emptyMap()))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.getMediaListForUser("").test().assertValues(listOf(media))
        mediaClient.getMediaListForUser("").test().assertValue(emptyList())
    }

    @Test
    fun `getMediaListFromSearch returns mapped response`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping()
        whenever(mediaInterface.getMediaListFromSearch("", 0, 1))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.getMediaListFromSearch("", 0, 1)
            .test()
            .assertValues(listOf(media))
    }

    @Test
    fun `fetchImagesForDepictedItem returns mapped response`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping()
        whenever(mediaInterface.fetchImagesForDepictedItem("haswbstatement:${BuildConfig.DEPICTS_PROPERTY}=", "0", "1"))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.fetchImagesForDepictedItem("", 0, 1)
            .test()
            .assertValues(listOf(media))
    }

    @Test
    fun `getMedia returns mapped response`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping()
        whenever(mediaInterface.getMedia("")).thenReturn(Single.just(mwQueryResponse))
        mediaClient.getMedia("").test().assertValues(media)
    }

    @Test
    fun `getPictureOfTheDay returns mapped response`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping()
        whenever(mediaInterface.getMediaWithGenerator(ArgumentMatchers.startsWith("Template:Potd/")))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient.getPictureOfTheDay().test().assertValues(media)
    }

    @Test
    fun `getPageHtml with null parse result returns empty`() {
        val mwParseResponse = mock<MwParseResponse>()
        whenever(mediaInterface.getPageHtml("")).thenReturn(Single.just(mwParseResponse))
        whenever(mwParseResponse.parse()).thenReturn(null)
        mediaClient.getPageHtml("").test().assertValues("")
    }

    @Test
    fun `getPageHtml with parse result returns text`() {
        val mwParseResponse = mock<MwParseResponse>()
        whenever(mediaInterface.getPageHtml("")).thenReturn(Single.just(mwParseResponse))
        val mwParseResult = mock<MwParseResult>()
        whenever(mwParseResponse.parse()).thenReturn(mwParseResult)
        whenever(mwParseResult.text()).thenReturn("text")
        mediaClient.getPageHtml("").test().assertValues("text")
    }

    @Test
    fun `getEntities throws exception for empty ids`() {
        mediaClient.getEntities(emptyList()).test().assertErrorMessage("empty list passed for ids")
    }

    @Test
    fun `getEntities invokes interface with non empty ids`() {
        val entities = mock<Entities>()
        whenever(mediaDetailInterface.getEntity("1|2")).thenReturn(Single.just(entities))
        mediaClient.getEntities(listOf("1","2")).test().assertValue(entities)
    }

    @Test
    fun `doesPageContainMedia returns false for empty items`() {
        val pageMediaListResponse = mock<PageMediaListResponse>()
        whenever(pageMediaInterface.getMediaList(""))
            .thenReturn(Single.just(pageMediaListResponse))
        whenever(pageMediaListResponse.items).thenReturn(emptyList())
        mediaClient.doesPageContainMedia("").test().assertValue(false)
    }

    @Test
    fun `doesPageContainMedia returns true for non empty items`() {
        val pageMediaListResponse = mock<PageMediaListResponse>()
        whenever(pageMediaInterface.getMediaList(""))
            .thenReturn(Single.just(pageMediaListResponse))
        whenever(pageMediaListResponse.items).thenReturn(listOf(mock()))
        mediaClient.doesPageContainMedia("").test().assertValue(true)
    }

    @Test
    fun getWikiText() {
        val wikiText = mock<MwQueryResponse>()
        whenever(mediaDetailInterface.getWikiText("File:Test.jpg")).thenReturn(Single.just(wikiText))
    }

    private fun mockQuery(queryReceiver: MwQueryResult.() -> Unit): MwQueryResponse {
        val mwQueryResponse = mock<MwQueryResponse>()
        val mwQueryResult = mock<MwQueryResult>()
        whenever(mwQueryResponse.query()).thenReturn(mwQueryResult)
        mwQueryResult.queryReceiver()
        return mwQueryResponse
    }

    private fun expectResponseWithPageId(expectedId: Int): MwQueryResponse {
        return mockQuery {
            val mwQueryPage = mock<MwQueryPage>()
            whenever(firstPage()).thenReturn(mwQueryPage)
            whenever(mwQueryPage.pageId()).thenReturn(expectedId)
        }
    }

    private fun expectSuccessfulMapping(continuationMap: Map<String, String>? = emptyMap()): Pair<MwQueryResponse, Media> {
        val media = media()
        val mwQueryPage = mock<MwQueryPage>()
        val mwQueryResponse = mockQuery {
            whenever(pages()).thenReturn(listOf(mwQueryPage, mwQueryPage))
            whenever(mwQueryPage.pageId()).thenReturn(1, 2)
        }
        whenever(mwQueryResponse.continuation()).thenReturn(continuationMap)
        val entities = mock<Entities>()
        whenever(mediaDetailInterface.getEntity("M1|M2")).thenReturn(Single.just(entities))
        val entity = mock<Entities.Entity>()
        whenever(entities.entities()).thenReturn(mapOf("a" to entity, "b" to entity))
        val imageInfo = mock<ImageInfo>()
        whenever(mwQueryPage.imageInfo()).thenReturn(imageInfo, null)
        whenever(mediaConverter.convert(mwQueryPage, entity, imageInfo)).thenReturn(media)
        return Pair(mwQueryResponse, media)
    }
}
