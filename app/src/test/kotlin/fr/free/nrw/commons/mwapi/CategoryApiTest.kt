package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import fr.free.nrw.commons.mwapi.model.Page
import fr.free.nrw.commons.mwapi.model.PageCategory
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryApiTest {
    private lateinit var server: MockWebServer
    private lateinit var url: String
    private lateinit var categoryApi: CategoryApi

    @Before
    fun setUp() {
        server = MockWebServer()
        url = "http://${server.hostName}:${server.port}/"
        categoryApi = CategoryApi(OkHttpClient.Builder().build(), Gson(), HttpUrl.parse(url))
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun apiReturnsEmptyListWhenError() {
        server.enqueue(MockResponse().setResponseCode(400).setBody(""))

        assertTrue(categoryApi.request("foo").blockingGet().isEmpty())
    }

    @Test
    fun apiReturnsEmptyWhenTheresNoQuery() {
        server.success(emptyMap())

        assertTrue(categoryApi.request("foo").blockingGet().isEmpty())
    }

    @Test
    fun apiReturnsEmptyWhenQueryHasNoPages() {
        server.success(mapOf("query" to emptyMap<String, Any>()))

        assertTrue(categoryApi.request("foo").blockingGet().isEmpty())
    }

    @Test
    fun apiReturnsEmptyWhenQueryHasPagesButTheyreEmpty() {
        server.success(mapOf("query" to
                mapOf("pages" to emptyList<String>())))

        assertTrue(categoryApi.request("foo").blockingGet().isEmpty())
    }

    @Test
    fun singlePageSingleCategory() {
        server.success(mapOf("query" to
                mapOf("pages" to listOf(
                        page(listOf("one"))
                ))))

        val response = categoryApi.request("foo").blockingGet()

        assertEquals(1, response.size)
        assertEquals("one", response[0])
    }

    @Test
    fun multiplePagesSingleCategory() {
        server.success(mapOf("query" to
                mapOf("pages" to listOf(
                        page(listOf("one")),
                        page(listOf("two"))
                ))))

        val response = categoryApi.request("foo").blockingGet()

        assertEquals(2, response.size)
        assertEquals("one", response[0])
        assertEquals("two", response[1])
    }

    @Test
    fun singlePageMultipleCategories() {
        server.success(mapOf("query" to
                mapOf("pages" to listOf(
                        page(listOf("one", "two"))
                ))))

        val response = categoryApi.request("foo").blockingGet()

        assertEquals(2, response.size)
        assertEquals("one", response[0])
        assertEquals("two", response[1])
    }

    @Test
    fun multiplePagesMultipleCategories() {
        server.success(mapOf("query" to
                mapOf("pages" to listOf(
                        page(listOf("one", "two")),
                        page(listOf("three", "four"))
                ))))

        val response = categoryApi.request("foo").blockingGet()

        assertEquals(4, response.size)
        assertEquals("one", response[0])
        assertEquals("two", response[1])
        assertEquals("three", response[2])
        assertEquals("four", response[3])
    }

    @Test
    fun multiplePagesMultipleCategories_duplicatesRemoved() {
        server.success(mapOf("query" to
                mapOf("pages" to listOf(
                        page(listOf("one", "two", "three")),
                        page(listOf("three", "four", "one"))
                ))))

        val response = categoryApi.request("foo").blockingGet()

        assertEquals(4, response.size)
        assertEquals("one", response[0])
        assertEquals("two", response[1])
        assertEquals("three", response[2])
        assertEquals("four", response[3])
    }

    @Test
    fun requestSendsWhatWeExpect() {
        server.success(mapOf("query" to mapOf("pages" to emptyList<String>())))

        val coords = "foo,bar"
        categoryApi.request(coords).blockingGet()

        server.takeRequest().let { request ->
            assertEquals("GET", request.method)
            assertEquals("/w/api.php", request.requestUrl.encodedPath())
            request.requestUrl.let { url ->
                assertEquals("query", url.queryParameter("action"))
                assertEquals("categories|coordinates|pageprops", url.queryParameter("prop"))
                assertEquals("json", url.queryParameter("format"))
                assertEquals("!hidden", url.queryParameter("clshow"))
                assertEquals("type|name|dim|country|region|globe", url.queryParameter("coprop"))
                assertEquals(coords, url.queryParameter("codistancefrompoint"))
                assertEquals("geosearch", url.queryParameter("generator"))
                assertEquals(coords, url.queryParameter("ggscoord"))
                assertEquals("10000", url.queryParameter("ggsradius"))
                assertEquals("10", url.queryParameter("ggslimit"))
                assertEquals("6", url.queryParameter("ggsnamespace"))
                assertEquals("type|name|dim|country|region|globe", url.queryParameter("ggsprop"))
                assertEquals("all", url.queryParameter("ggsprimary"))
                assertEquals("2", url.queryParameter("formatversion"))
            }
        }
    }

    private fun page(catList: List<String>) = Page().apply {
        categories = catList.map {
            PageCategory().apply {
                title = "Category:$it"
            }
        }.toTypedArray()
    }
}

fun MockWebServer.success(response: Map<String, Any>) {
    enqueue(MockResponse().setResponseCode(200).setBody(Gson().toJson(response)))
}