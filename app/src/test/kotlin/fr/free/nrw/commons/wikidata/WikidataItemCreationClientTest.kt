package fr.free.nrw.commons.wikidata

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.WbCreateEntityResponse
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class WikidataItemCreationClientTest {
    @Mock
    private lateinit var wikidataInterface: WikidataInterface

    @Mock
    private lateinit var csrfTokenClient: CsrfTokenClient

    private lateinit var client: WikidataItemCreationClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        client = WikidataItemCreationClient(wikidataInterface, Gson(), csrfTokenClient)
    }

    @Test
    fun `createItem posts label and sitelink and returns new QID`() {
        val entity = mock<Entities.Entity>()
        whenever(entity.id()).thenReturn("Q123")
        val response = mock<WbCreateEntityResponse>()
        whenever(response.entity).thenReturn(entity)
        whenever(csrfTokenClient.getTokenBlocking()).thenReturn("token")
        whenever(wikidataInterface.postCreateItem(anyString(), eq("token"))).thenReturn(Single.just(response))

        client.createItem("ja", "タイトル").test().assertValue("Q123")

        val data = argumentCaptor<String>()
        verify(wikidataInterface).postCreateItem(data.capture(), eq("token"))
        assert(data.firstValue.contains("\"ja\":{\"language\":\"ja\",\"value\":\"タイトル\"}"))
        assert(data.firstValue.contains("\"jawiki\":{\"site\":\"jawiki\",\"title\":\"タイトル\"}"))
    }

    @Test
    fun `createItem fails when Wikidata returns no entity`() {
        whenever(csrfTokenClient.getTokenBlocking()).thenReturn("token")
        whenever(wikidataInterface.postCreateItem(anyString(), eq("token")))
            .thenReturn(Single.just(mock<WbCreateEntityResponse>()))
        client.createItem("en", "Title").test().assertError(IllegalStateException::class.java)
    }
}
