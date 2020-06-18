package fr.free.nrw.commons.explore.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.wikidata.Entities

class PageableMediaDataSourceTest {
    @Mock
    lateinit var mediaConverter: MediaConverter
    @Mock
    lateinit var mediaClient: MediaClient

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `loadFunction invokes mediaClient and has Label`() {
        val (media, entity: Entities.Entity) = expectMediaAndEntity()
        val label: Entities.Label = mock()
        whenever(entity.labels()).thenReturn(mapOf(" " to label))
        whenever(label.value()).thenReturn("label")
        val pageableMediaDataSource = PageableMediaDataSource(mock(), mediaConverter, mediaClient)
        pageableMediaDataSource.onQueryUpdated("test")
        assertThat(pageableMediaDataSource.loadFunction(0,1), `is`(listOf(media)))
        verify(media).caption = "label"
    }

    @Test
    fun `loadFunction invokes mediaClient and does not have Label`() {
        val (media, entity: Entities.Entity) = expectMediaAndEntity()
        whenever(entity.labels()).thenReturn(mapOf())
        val pageableMediaDataSource = PageableMediaDataSource(mock(), mediaConverter, mediaClient)
        pageableMediaDataSource.onQueryUpdated("test")
        assertThat(pageableMediaDataSource.loadFunction(0,1), `is`(listOf(media)))
        verify(media).caption = MediaClient.NO_CAPTION
    }

    private fun expectMediaAndEntity(): Pair<Media, Entities.Entity> {
        val queryResponse: MwQueryResponse = mock()
        whenever(mediaClient.getMediaListFromSearch("test", 0, 1))
            .thenReturn(Single.just(queryResponse))
        val queryResult: MwQueryResult = mock()
        whenever(queryResponse.query()).thenReturn(queryResult)
        val queryPage: MwQueryPage = mock()
        whenever(queryResult.pages()).thenReturn(listOf(queryPage))
        val media = mock<Media>()
        whenever(mediaConverter.convert(queryPage)).thenReturn(media)
        whenever(media.pageId).thenReturn("1")
        val entities: Entities = mock()
        whenever(mediaClient.getEntities("${PAGE_ID_PREFIX}1")).thenReturn(Single.just(entities))
        val entity: Entities.Entity = mock()
        whenever(entities.entities()).thenReturn(mapOf("" to entity))
        return Pair(media, entity)
    }
}
