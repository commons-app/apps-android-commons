package fr.free.nrw.commons

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.SparqlResponse
import fr.free.nrw.commons.mwapi.WikidataQueryClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.model.NearbyQueryParams
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Checks entity query parameters and response handling against a local HTTP server. */
class WikidataQueryClientTest {
    @get:Rule
    val server = MockWebServer()

    private val depictsClient: DepictsClient = mock()

    /** Both hierarchy requests preserve relation direction, paging and the existing depiction mapper. */
    @Test
    fun hierarchyQueriesKeepDirectionAndPaging() {
        val depictedItem: DepictedItem = mock()
        whenever(depictsClient.toDepictions(any())).thenAnswer { invocation ->
            invocation.getArgument<Single<SparqlResponse>>(0).map { response ->
                assertEquals(
                    "Q2",
                    response.results.bindings
                        .single()
                        .id,
                )
                listOf(depictedItem)
            }
        }
        val json = """{"results":{"bindings":[{"item":{"type":"uri","value":"http://www.wikidata.org/entity/Q2"}}]}}"""
        repeat(2) { server.enqueue(MockResponse().setBody(json)) }
        val client = WikidataQueryClient(OkHttpClient(), depictsClient, server.url("/sparql").toString(), Gson())

        client.getChildDepictions("Q1", 8, 4).test().assertResult(listOf(depictedItem))
        client.getParentDepictions("Q1", 12, 6).test().assertResult(listOf(depictedItem))

        val child = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)?.requestUrl)
        val parent = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)?.requestUrl)
        assertEquals("json", child.queryParameter("format"))
        assertEquals("json", parent.queryParameter("format"))
        assertTrue(child.queryParameter("query")!!.contains("?item wdt:P279 wd:Q1."))
        assertTrue(parent.queryParameter("query")!!.contains("wd:Q1 wdt:P279 ?item."))
        assertTrue(child.queryParameter("query")!!.contains(Regex("LIMIT\\s+4\\s+OFFSET\\s+8")))
        assertTrue(parent.queryParameter("query")!!.contains(Regex("LIMIT\\s+6\\s+OFFSET\\s+12")))
    }

    /** Details requests retain the requested item IDs and language when Wikidata returns no rows. */
    @Test
    fun placeDetailsQueryUsesItemIdsAndLanguage() {
        server.enqueue(MockResponse().setBody("""{"results":{"bindings":[]}}"""))
        val place: Place = mock()
        whenever(place.wikiDataEntityId).thenReturn("Q42")
        val client = WikidataQueryClient(OkHttpClient(), depictsClient, server.url("/sparql").toString(), Gson())

        assertEquals(emptyList<Place>(), client.getPlaces(listOf(place), "de"))

        val url = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)?.requestUrl)
        assertEquals("json", url.queryParameter("format"))
        val query = requireNotNull(url.queryParameter("query"))
        assertTrue(query.contains("wd:Q42"))
        assertTrue(query.contains("wikibase:language \"de,en,fr"))
    }

    /** Reads the radial count and keeps the existing error contract for rectangular queries. */
    @Test
    fun nearbyCountParsesBindingsAndReportsHttpErrors() {
        server.enqueue(MockResponse().setBody("""{"results":{"bindings":[{"itemCount":{"value":"12"}}]}}"""))
        server.enqueue(MockResponse().setStatus("HTTP/1.1 503 Test service unavailable"))
        val client = WikidataQueryClient(OkHttpClient(), depictsClient, server.url("/sparql").toString(), Gson())
        val center = LatLng(1.25, 2.5, 0f)
        val edge = LatLng(3.0, 4.0, 0f)

        assertEquals(12, client.getNearbyItemCount(NearbyQueryParams.Radial(center, 5f)))
        val radial = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)?.requestUrl?.queryParameter("query"))
        assertTrue(radial.contains("Point(2.5000 1.2500)"))
        assertTrue(radial.contains("wikibase:radius \"5.00\""))

        val failure =
            assertThrows(IOException::class.java) {
                client.getNearbyItemCount(NearbyQueryParams.Rectangular(center, edge))
            }
        assertEquals("Test service unavailable", failure.message)
        val rectangle = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)?.requestUrl?.queryParameter("query"))
        assertTrue(rectangle.contains("wikibase:box"))
    }
}
