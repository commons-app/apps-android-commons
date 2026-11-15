package fr.free.nrw.commons.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.media.MediaConverter
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.gallery.ImageInfo
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import io.reactivex.Single
import media
import org.junit.Test

class MediaCollectionClientTest {
    private val mediaInterface: MediaInterface = mock()
    private val mediaDetailInterface: MediaDetailInterface = mock()
    private val mediaConverter: MediaConverter = mock()
    private val mediaClient =
        MediaCollectionClient(mediaInterface, MediaResponseMapper(mediaDetailInterface, mediaConverter))
    private val continuationMap = mapOf("continuation" to "continuation")

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
        val (resetMwQueryResponse, resetMedia) = expectSuccessfulMapping()
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
        mediaClient
            .getMediaListFromSearch("", 0, 1)
            .test()
            .assertValues(listOf(media))
    }

    @Test
    fun `fetchImagesForDepictedItem returns mapped response`() {
        val (mwQueryResponse, media) = expectSuccessfulMapping()
        whenever(mediaInterface.fetchImagesForDepictedItem("haswbstatement:${BuildConfig.DEPICTS_PROPERTY}=", "0", "1"))
            .thenReturn(Single.just(mwQueryResponse))
        mediaClient
            .fetchImagesForDepictedItem("", 0, 1)
            .test()
            .assertValues(listOf(media))
    }

    @Test
    fun `user reset starts fetching again after the last page`() {
        val (response, media) = expectSuccessfulMapping(null)
        whenever(mediaInterface.getMediaListForUser("user", 10, emptyMap()))
            .thenReturn(Single.just(response))
        mediaClient.getMediaListForUser("user").test().assertValue(listOf(media))
        mediaClient.getMediaListForUser("user").test().assertValue(emptyList())

        mediaClient.resetUserNameContinuation("user")
        val (resetResponse, resetMedia) = expectSuccessfulMapping(null)
        whenever(mediaInterface.getMediaListForUser("user", 10, emptyMap()))
            .thenReturn(Single.just(resetResponse))
        mediaClient.getMediaListForUser("user").test().assertValue(listOf(resetMedia))
    }

    @Test
    fun `category and user continuation are independent for the same name`() {
        val response = mock<MwQueryResponse>()
        whenever(response.continuation()).thenReturn(continuationMap)
        whenever(mediaInterface.getMediaListFromCategory("same", 10, emptyMap()))
            .thenReturn(Single.just(response))
        whenever(mediaInterface.getMediaListForUser("same", 10, emptyMap()))
            .thenReturn(Single.just(response))
        mediaClient.getMediaListFromCategory("same").test().assertValue(emptyList())
        mediaClient.getMediaListForUser("same").test().assertValue(emptyList())
    }

    @Test
    fun `geosearch keeps its result limit and radius`() {
        val (response, media) = expectSuccessfulMapping()
        whenever(mediaInterface.getMediaListFromGeoSearch("1|2", 30, 10000))
            .thenReturn(Single.just(response))
        mediaClient.getMediaListFromGeoSearch("1|2").test().assertValue(listOf(media))
    }

    private fun mockQuery(queryReceiver: MwQueryResult.() -> Unit): MwQueryResponse {
        val mwQueryResponse = mock<MwQueryResponse>()
        val mwQueryResult = mock<MwQueryResult>()
        whenever(mwQueryResponse.query()).thenReturn(mwQueryResult)
        mwQueryResult.queryReceiver()
        return mwQueryResponse
    }

    private fun expectSuccessfulMapping(
        continuationMap: Map<String, String>? = emptyMap(),
    ): Pair<MwQueryResponse, Media> {
        val media = media()
        val mwQueryPage = mock<MwQueryPage>()
        val mwQueryResponse =
            mockQuery {
                whenever(pages()).thenReturn(listOf(mwQueryPage, mwQueryPage))
                whenever(mwQueryPage.pageId()).thenReturn(1, 2)
            }
        whenever(mwQueryResponse.continuation()).thenReturn(continuationMap)
        val entities = mock<Entities>()
        whenever(mediaDetailInterface.getEntity("M1|M2")).thenReturn(Single.just(entities))
        val entity = mock<Entities.Entity>()
        whenever(entities.getEntityMap()).thenReturn(mapOf("a" to entity, "b" to entity))
        val imageInfo = mock<ImageInfo>()
        whenever(mwQueryPage.imageInfo()).thenReturn(imageInfo, null)
        whenever(mediaConverter.convert(mwQueryPage, entity, imageInfo)).thenReturn(media)
        return Pair(mwQueryResponse, media)
    }
}
