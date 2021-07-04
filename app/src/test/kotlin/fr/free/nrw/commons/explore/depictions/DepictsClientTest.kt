package fr.free.nrw.commons.explore.depictions

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import depictSearchItem
import fr.free.nrw.commons.mwapi.Binding
import fr.free.nrw.commons.mwapi.Result
import fr.free.nrw.commons.mwapi.SparqlResponse
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import fr.free.nrw.commons.wikidata.model.DepictSearchResponse
import io.reactivex.Single
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.wikipedia.wikidata.Entities

class DepictsClientTest {

    @Mock
    private lateinit var depictsInterface: DepictsInterface
    private lateinit var depictsClient: DepictsClient

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        depictsClient = DepictsClient(depictsInterface)
    }

    @Test
    @Ignore()
    fun searchForDepictions() {
        val depictSearchResponse = mock<DepictSearchResponse>()
        whenever(depictsInterface.searchForDepicts("query", "1", "en", "en", "0"))
            .thenReturn(Single.just(depictSearchResponse))
        whenever(depictSearchResponse.search).thenReturn(listOf(depictSearchItem("1"),depictSearchItem("2")))
        val entities = mock<Entities>()
        whenever(depictsInterface.getEntities("1|2")).thenReturn(Single.just(entities))
        whenever(entities.entities()).thenReturn(emptyMap())
        depictsClient.searchForDepictions("query", 1, 0)
            .test()
            .assertValue(emptyList())
    }


    @Test
    fun getEntities() {
        val entities = mock<Entities>()
        whenever(depictsInterface.getEntities("ids")).thenReturn(Single.just(entities))
        depictsClient.getEntities("ids").test().assertValue(entities)
    }

    @Test
    fun toDepictions() {
        val sparqlResponse = mock<SparqlResponse>()
        val result = mock<Result>()
        whenever(sparqlResponse.results).thenReturn(result)
        val binding1 = mock<Binding>()
        val binding2 = mock<Binding>()
        whenever(result.bindings).thenReturn(listOf(binding1, binding2))
        whenever(binding1.id).thenReturn("1")
        whenever(binding2.id).thenReturn("2")
        val entities = mock<Entities>()
        whenever(depictsInterface.getEntities("1|2")).thenReturn(Single.just(entities))
        whenever(entities.entities()).thenReturn(emptyMap())
        depictsClient.toDepictions(Single.just(sparqlResponse))
            .test()
            .assertValue(emptyList())
    }
}
