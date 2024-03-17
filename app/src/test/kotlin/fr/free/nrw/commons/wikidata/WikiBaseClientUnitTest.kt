package fr.free.nrw.commons.wikidata

import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.upload.WikiBaseInterface
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import io.reactivex.Observable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class WikiBaseClientUnitTest {

    private val csrfTokenClient: CsrfTokenClient = mock()
    private val wikiBaseInterface: WikiBaseInterface = mock()
    private val wikiBaseClient = WikiBaseClient(wikiBaseInterface, csrfTokenClient)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        whenever(csrfTokenClient.getTokenBlocking()).thenReturn("test")
    }

    @Test
    fun testPostEditEntity() {
        val response = mock<MwPostResponse>()
        whenever(response.successVal).thenReturn(1)
        whenever(wikiBaseInterface.postEditEntity("the-id", "test", "the-data"))
            .thenReturn(Observable.just(response))

        val result = wikiBaseClient.postEditEntity("the-id", "the-data").blockingFirst()

        assertTrue(result)
    }

    @Test
    fun testPostEditEntityByFilename() {
        val response = mock<MwPostResponse>()
        whenever(response.successVal).thenReturn(1)
        whenever(wikiBaseInterface.postEditEntityByFilename("File:Example.jpg", "test", "the-data"))
            .thenReturn(Observable.just(response))

        val result = wikiBaseClient.postEditEntityByFilename("File:Example.jpg", "the-data").blockingFirst()

        assertTrue(result)
    }

    @Test
    fun getFileEntityId() {
        val upload = UploadResult("result", "key", 0, "Example.jpg")

        val response = mock<MwQueryResponse>()
        val query = mock<MwQueryResult>()
        val page = mock<MwQueryPage>()
        whenever(response.query()).thenReturn(query)
        whenever(query.pages()).thenReturn(mutableListOf(page))
        whenever(page.pageId()).thenReturn(123)
        whenever(wikiBaseInterface.getFileEntityId("File:Example.jpg"))
            .thenReturn(Observable.just(response))

        val result = wikiBaseClient.getFileEntityId(upload).blockingFirst()

        assertEquals(123L, result)
    }

    @Test
    fun addLabelstoWikidata() {
        val mwPostResponse = mock<MwPostResponse>()
        whenever(wikiBaseInterface.addLabelstoWikidata(
            "M123", "test", "en", "caption"
        )).thenReturn(Observable.just(mwPostResponse))

        val result = wikiBaseClient.addLabelstoWikidata(123L, "en", "caption").blockingFirst()

        assertSame(mwPostResponse, result)
    }
}