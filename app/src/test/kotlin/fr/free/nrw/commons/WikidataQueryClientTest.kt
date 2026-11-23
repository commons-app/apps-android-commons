package fr.free.nrw.commons

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.SparqlResponse
import fr.free.nrw.commons.mwapi.SparqlResponseException
import fr.free.nrw.commons.mwapi.WikidataQueryClient
import fr.free.nrw.commons.mwapi.retryAfterMillis
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.model.NearbyQueryParams
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Single
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.Date
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

    /** A refused details request keeps the status code and the wait asked for by the service. */
    @Test
    fun placeDetailsErrorKeepsStatusCodeAndRetryAfter() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "7").setBody("Rate limit exceeded"))
        val place: Place = mock()
        whenever(place.wikiDataEntityId).thenReturn("Q42")
        val client = WikidataQueryClient(OkHttpClient(), depictsClient, server.url("/sparql").toString(), Gson())

        val error = assertThrows(SparqlResponseException::class.java) { client.getPlaces(listOf(place), "en") }

        assertEquals(429, error.code)
        assertEquals(7000L, error.retryAfterMillis)
        assertEquals("Unexpected response code: 429", error.message)
    }

    /** Retry-After is read in seconds or as a date, a wait in the past is no wait. */
    @Test
    fun retryAfterIsReadInSecondsOrAsADate() {
        val now = 1_700_000_000_000L

        fun wait(value: String?) =
            Headers
                .Builder()
                .apply { if (value != null) add("Retry-After", value) }
                .build()
                .retryAfterMillis(now)

        fun waitUntil(date: Long) =
            Headers
                .Builder()
                .add("Retry-After", Date(date))
                .build()
                .retryAfterMillis(now)

        assertEquals(120_000L, wait(" 120 "))
        assertEquals(30_000L, waitUntil(now + 30_000L))
        assertEquals(0L, wait("-5"))
        assertEquals(0L, waitUntil(now - 30_000L))
        assertNull(wait("soon"))
        assertNull(wait(null))
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
