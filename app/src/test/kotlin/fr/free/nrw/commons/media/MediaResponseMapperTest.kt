package fr.free.nrw.commons.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.explore.media.MediaConverter
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import io.reactivex.Single
import org.junit.Test
import org.mockito.Mockito.verifyNoInteractions

class MediaResponseMapperTest {
    private val mediaDetailInterface: MediaDetailInterface = mock()
    private val mediaConverter: MediaConverter = mock()
    private val mapper = MediaResponseMapper(mediaDetailInterface, mediaConverter)

    @Test
    fun `missing query returns an empty list without fetching entities`() {
        mapper
            .map(Single.just(mock()))
            .test()
            .assertValue(emptyList())
            .assertComplete()
        verifyNoInteractions(mediaDetailInterface, mediaConverter)
    }

    @Test
    fun `empty pages return an empty list without fetching entities`() {
        val response = responseWithPages(emptyList())
        mapper
            .map(Single.just(response))
            .test()
            .assertValue(emptyList())
            .assertComplete()
        verifyNoInteractions(mediaDetailInterface, mediaConverter)
    }

    @Test
    fun `deleted pages retain the deleted file error`() {
        val page: MwQueryPage = mock()
        whenever(page.missing()).thenReturn(true)
        mapper
            .map(Single.just(responseWithPages(listOf(page))))
            .test()
            .assertError(FileDeletedOnCommonsException::class.java)
        verifyNoInteractions(mediaDetailInterface, mediaConverter)
    }

    @Test
    fun `network errors propagate unchanged`() {
        val error = IllegalStateException("network failed")
        mapper.map(Single.error(error)).test().assertError(error)
        verifyNoInteractions(mediaDetailInterface, mediaConverter)
    }

    private fun responseWithPages(pages: List<MwQueryPage>): MwQueryResponse {
        val response: MwQueryResponse = mock()
        val query: MwQueryResult = mock()
        whenever(response.query()).thenReturn(query)
        whenever(query.pages()).thenReturn(pages)
        return response
    }
}
