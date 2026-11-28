package fr.free.nrw.commons.category

import fr.free.nrw.commons.wikidata.GsonUtil.defaultGson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Runs [CategoryRedirectResolver] against a local web server, so the real request parameters
 * and the parsing of real Commons responses are checked as well.
 */
class CategoryRedirectResolverTest {
    private lateinit var server: MockWebServer
    private lateinit var resolver: CategoryRedirectResolver

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val categoryInterface =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(defaultGson))
                .build()
                .create(CategoryInterface::class.java)
        resolver = CategoryRedirectResolver(categoryInterface)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String): String =
        javaClass.classLoader!!
            .getResourceAsStream("category_redirects/$name")!!
            .bufferedReader()
            .use { it.readText() }

    private fun enqueue(body: String) {
        server.enqueue(MockResponse().setBody(body))
    }

    private fun RecordedRequest.queryParameter(name: String): String? = requestUrl!!.queryParameter(name)

    private fun resolve(vararg categories: String): List<String> = resolver.resolve(categories.toList()).blockingGet()

    @Test
    fun `soft redirect from the issue resolves to the category it points to`() {
        // Real responses from Commons for Category:Aerial photos of California
        enqueue(fixture("redirect_info.json"))
        enqueue(fixture("redirect_links.json"))

        assertEquals(
            listOf("Aerial photographs of California"),
            resolve("Aerial photos of California"),
        )

        val infoRequest = server.takeRequest()
        assertEquals("Category:Aerial photos of California", infoRequest.queryParameter("titles"))
        assertEquals("Category:Category redirects", infoRequest.queryParameter("clcategories"))
        assertEquals("1", infoRequest.queryParameter("redirects"))
        val linksRequest = server.takeRequest()
        assertEquals("links", linksRequest.queryParameter("prop"))
        assertEquals("14", linksRequest.queryParameter("plnamespace"))
        assertEquals("Category:Aerial photos of California", linksRequest.queryParameter("titles"))
    }

    @Test
    fun `regular category is kept and needs no link lookup`() {
        enqueue(
            """{"query":{"pages":[{"ns":14,"title":"Category:Aerial photographs of California"}]}}""",
        )

        assertEquals(
            listOf("Aerial photographs of California"),
            resolve("Aerial photographs of California"),
        )
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `selecting both the redirect and its target keeps the target once`() {
        enqueue(fixture("redirect_info.json"))
        enqueue(fixture("redirect_links.json"))

        assertEquals(
            listOf("Aerial photographs of California"),
            resolve("Aerial photos of California", "Aerial photographs of California"),
        )
    }

    @Test
    fun `other categories keep their order around a resolved redirect`() {
        enqueue(
            """
            {"query":{"pages":[
              {"ns":14,"title":"Category:Bridges"},
              {"ns":14,"title":"Category:Aerial photos of California",
               "categories":[{"ns":14,"title":"Category:Category redirects"}]},
              {"ns":14,"title":"Category:Rivers"}
            ]}}
            """.trimIndent(),
        )
        enqueue(fixture("redirect_links.json"))

        assertEquals(
            listOf("Bridges", "Aerial photographs of California", "Rivers"),
            resolve("Bridges", "Aerial photos of California", "Rivers"),
        )
    }

    @Test
    fun `differently written name is normalized and then resolved`() {
        // The real response normalizes "aerial_photos_of_California" to the redirect's title
        enqueue(fixture("redirect_info.json"))
        enqueue(fixture("redirect_links.json"))

        assertEquals(
            listOf("Aerial photographs of California"),
            resolve("aerial_photos_of_California"),
        )
    }

    @Test
    fun `hard redirect resolves to its target`() {
        enqueue(
            """
            {"query":{
              "redirects":[{"from":"Category:Old name","to":"Category:New name"}],
              "pages":[{"ns":14,"title":"Category:New name"}]
            }}
            """.trimIndent(),
        )

        assertEquals(listOf("New name"), resolve("Old name"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `soft redirect that links to several categories is left unchanged`() {
        enqueue(
            """
            {"query":{"pages":[{"ns":14,"title":"Category:Ambiguous",
              "categories":[{"ns":14,"title":"Category:Category redirects"}]}]}}
            """.trimIndent(),
        )
        enqueue(
            """
            {"query":{"pages":[{"ns":14,"title":"Category:Ambiguous",
              "links":[{"ns":14,"title":"Category:First"},{"ns":14,"title":"Category:Second"}]}]}}
            """.trimIndent(),
        )

        assertEquals(listOf("Ambiguous"), resolve("Ambiguous"))
    }

    @Test
    fun `server error keeps the categories as they are`() {
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(
            listOf("Aerial photos of California", "Bridges"),
            resolve("Aerial photos of California", "Bridges"),
        )
    }

    @Test
    fun `failed link lookup keeps the categories as they are`() {
        enqueue(fixture("redirect_info.json"))
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(listOf("Aerial photos of California"), resolve("Aerial photos of California"))
    }

    @Test
    fun `api error response keeps the categories as they are`() {
        enqueue("""{"error":{"code":"toomanyvalues","info":"Too many values"}}""")

        assertEquals(listOf("Aerial photos of California"), resolve("Aerial photos of California"))
    }

    @Test
    fun `no categories needs no request`() {
        assertEquals(emptyList<String>(), resolve())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `more than fifty categories are checked in batches of fifty`() {
        val categories = (1..51).map { "Category $it" }
        enqueue("""{"query":{"pages":[]}}""")
        enqueue("""{"query":{"pages":[]}}""")

        assertEquals(categories, resolver.resolve(categories).blockingGet())

        assertEquals(2, server.requestCount)
        val first = server.takeRequest().queryParameter("titles")!!.split("|")
        val second = server.takeRequest().queryParameter("titles")!!.split("|")
        assertEquals(50, first.size)
        assertEquals(listOf("Category:Category 51"), second)
        assertTrue(first.all { it.startsWith("Category:") })
    }
}
