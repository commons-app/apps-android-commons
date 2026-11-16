package fr.free.nrw.commons.wikidata

import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.upload.WikiBaseInterface
import fr.free.nrw.commons.upload.depicts.Claims
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import io.reactivex.Observable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
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
    fun getClaimIdsByProperty_fileWithoutStructuredDataHasNoClaims() {
        whenever(wikiBaseInterface.getClaimsByProperty("M123", "P180"))
            .thenReturn(Observable.error(mwException(NO_SUCH_ENTITY)))

        val result = wikiBaseClient.getClaimIdsByProperty("M123", "P180").blockingFirst()

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun getClaimIdsByProperty_otherErrorsArePassedOn() {
        val error = mwException(PARAM_INVALID)
        whenever(wikiBaseInterface.getClaimsByProperty("M123", "P180"))
            .thenReturn(Observable.error(error))

        val result = wikiBaseClient.getClaimIdsByProperty("M123", "P180").test()

        result.assertError(error)
    }

    @Test
    fun getClaimItemIdsByProperty_readsTheDepictedItems() {
        whenever(wikiBaseInterface.getClaimsByProperty("M123", "P180"))
            .thenReturn(Observable.just(GsonUtil.defaultGson.fromJson(DEPICTS_CLAIMS, Claims::class.java)))

        val result = wikiBaseClient.getClaimItemIdsByProperty("M123", "P180").blockingFirst()

        // The "unknown value" claim has no item, so it is left out
        assertEquals(listOf("Q146"), result)
    }

    @Test
    fun getFileEntityId() {
        val response = mock<MwQueryResponse>()
        val query = mock<MwQueryResult>()
        val page = mock<MwQueryPage>()
        whenever(response.query()).thenReturn(query)
        whenever(query.pages()).thenReturn(mutableListOf(page))
        whenever(page.pageId()).thenReturn(123)
        whenever(wikiBaseInterface.getFileEntityId("File:Example.jpg"))
            .thenReturn(Observable.just(response))

        val result = wikiBaseClient.getFileEntityId("Example.jpg").blockingFirst()

        assertEquals(123L, result)
    }

    @Test
    fun addLabelstoWikidata() {
        val mwPostResponse = mock<MwPostResponse>()
        whenever(
            wikiBaseInterface.addLabelstoWikidata(
                "M123",
                "test",
                "en",
                "caption",
            ),
        ).thenReturn(Observable.just(mwPostResponse))

        val result = wikiBaseClient.addLabelsToWikidata(123L, "en", "caption").blockingFirst()

        assertSame(mwPostResponse, result)
    }

    /**
     * Reads an "errors" answer from Commons, the same way Retrofit does, and returns the exception
     */
    private fun mwException(json: String): MwException =
        assertThrows(MwException::class.java) {
            GsonUtil.defaultGson.fromJson(json, Claims::class.java)
        }

    private companion object {
        // Answer from Commons to action=wbgetclaims for a file depicting a cat and an unknown value
        const val DEPICTS_CLAIMS =
            """{"claims":{"P180":[""" +
                """{"mainsnak":{"snaktype":"value","property":"P180","datavalue":{"value":""" +
                """{"entity-type":"item","numeric-id":146,"id":"Q146"},"type":"wikibase-entityid"},""" +
                """"datatype":"wikibase-item"},"type":"statement","id":"M123-a","rank":"normal"},""" +
                """{"mainsnak":{"snaktype":"somevalue","property":"P180","datatype":"wikibase-item"},""" +
                """"type":"statement","id":"M123-b","rank":"normal"}]}}"""

        // Answers from Commons to action=wbgetclaims with errorformat=plaintext
        const val NO_SUCH_ENTITY =
            """{"errors":[{"code":"no-such-entity","text":"⧼no-such-entity⧽","module":"main"}]}"""
        const val PARAM_INVALID =
            """{"errors":[{"code":"param-invalid","text":"The serialization \"M0\" is not recognized""" +
                """ by the configured id builders","module":"wbgetclaims"}]}"""
    }
}
