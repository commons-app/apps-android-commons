package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient.mapType
import fr.free.nrw.commons.utils.CommonsDateUtil
import junit.framework.Assert.assertEquals
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*
import kotlin.random.Random

/**
 * Mock web server based tests for ok http json api client
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [23], application = TestCommonsApplication::class)
class OkHttpJsonApiClientTest {

    private lateinit var testObject: OkHttpJsonApiClient
    private lateinit var toolsForgeServer: MockWebServer
    private lateinit var sparqlServer: MockWebServer
    private lateinit var campaignsServer: MockWebServer
    private lateinit var server: MockWebServer
    private lateinit var sharedPreferences: JsonKvStore
    private lateinit var okHttpClient: OkHttpClient

    /**
     * - make instances of mock web server
     * - create instance of OkHttpJsonApiClient
     */
    @Before
    fun setUp() {
        server = MockWebServer()
        toolsForgeServer = MockWebServer()
        sparqlServer = MockWebServer()
        campaignsServer = MockWebServer()
        okHttpClient = OkHttpClient.Builder().build()
        sharedPreferences = Mockito.mock(JsonKvStore::class.java)
        val toolsForgeUrl = "http://" + toolsForgeServer.hostName + ":" + toolsForgeServer.port + "/"
        val sparqlUrl = "http://" + sparqlServer.hostName + ":" + sparqlServer.port + "/"
        val campaignsUrl = "http://" + campaignsServer.hostName + ":" + campaignsServer.port + "/"
        val serverUrl = "http://" + server.hostName + ":" + server.port + "/"
        testObject = OkHttpJsonApiClient(okHttpClient, HttpUrl.get(toolsForgeUrl), sparqlUrl, campaignsUrl, serverUrl, sharedPreferences, Gson())
    }

    /**
     * Shutdown server after tests
     */
    @After
    fun teardown() {
        server.shutdown()
        toolsForgeServer.shutdown()
        sparqlServer.shutdown()
        campaignsServer.shutdown()
    }

    /**
     * Test response for category images
     */
    @Test
    fun getCategoryImages() {
        server.enqueue(getFirstPageOfImages())
        testFirstPageQuery()
    }

    /**
     * test paginated response for category images
     */
    @Test
    fun getCategoryImagesWithContinue() {
        server.enqueue(getFirstPageOfImages())
        server.enqueue(getSecondPageOfImages())
        testFirstPageQuery()

        `when`(sharedPreferences.getJson<HashMap<String, String>>("query_continue_Watercraft moored off shore", mapType))
                .thenReturn(hashMapOf(Pair("gcmcontinue", "testvalue"), Pair("continue", "gcmcontinue||")))


        val categoryImagesContinued = testObject.getMediaList("category", "Watercraft moored off shore")!!.blockingGet()

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("json", body["format"])
                Assert.assertEquals("2", body["formatversion"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("categorymembers", body["generator"])
                Assert.assertEquals("file", body["gcmtype"])
                Assert.assertEquals("Watercraft moored off shore", body["gcmtitle"])
                Assert.assertEquals("timestamp", body["gcmsort"])
                Assert.assertEquals("desc", body["gcmdir"])
                Assert.assertEquals("testvalue", body["gcmcontinue"])
                Assert.assertEquals("gcmcontinue||", body["continue"])
                Assert.assertEquals("imageinfo", body["prop"])
                Assert.assertEquals("url|extmetadata", body["iiprop"])
                Assert.assertEquals("DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl", body["iiextmetadatafilter"])
            }
        }

        assertEquals(categoryImagesContinued.size, 2)
    }

    /**
     * Test response for search images
     */
    @Test
    fun getSearchImages() {
        server.enqueue(getFirstPageOfImages())
        testFirstPageSearchQuery()
    }

    /**
     * Test response for paginated search
     */
    @Test
    fun getSearchImagesWithContinue() {
        server.enqueue(getFirstPageOfSearchImages())
        server.enqueue(getSecondPageOfSearchImages())
        testFirstPageSearchQuery()

        `when`(sharedPreferences.getJson<HashMap<String, String>>("query_continue_Watercraft moored off shore", mapType))
                .thenReturn(hashMapOf(Pair("gsroffset", "25"), Pair("continue", "gsroffset||")))


        val categoryImagesContinued = testObject.getMediaList("search", "Watercraft moored off shore")!!.blockingGet()

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("json", body["format"])
                Assert.assertEquals("2", body["formatversion"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("search", body["generator"])
                Assert.assertEquals("text", body["gsrwhat"])
                Assert.assertEquals("6", body["gsrnamespace"])
                Assert.assertEquals("25", body["gsrlimit"])
                Assert.assertEquals("Watercraft moored off shore", body["gsrsearch"])
                Assert.assertEquals("25", body["gsroffset"])
                Assert.assertEquals("gsroffset||", body["continue"])
                Assert.assertEquals("imageinfo", body["prop"])
                Assert.assertEquals("url|extmetadata", body["iiprop"])
                Assert.assertEquals("DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl", body["iiextmetadatafilter"])
            }
        }

        assertEquals(categoryImagesContinued.size, 2)
    }

    /**
     * Test response for getting media without generator
     */
    @Test
    fun getMedia() {
        server.enqueue(getMediaList("", "", "", 1))

        val media = testObject.getMedia("Test.jpg", false)!!.blockingGet()

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("json", body["format"])
                Assert.assertEquals("2", body["formatversion"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("Test.jpg", body["titles"])
                Assert.assertEquals("imageinfo", body["prop"])
                Assert.assertEquals("url|extmetadata", body["iiprop"])
                Assert.assertEquals("DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl", body["iiextmetadatafilter"])
            }
        }

        assert(media is Media)
    }

    /**
     * Test response for getting media with generator
     * Equivalent of testing POTD
     */
    @Test
    fun getImageWithGenerator() {
        val template = "Template:Potd/" + CommonsDateUtil.getIso8601DateFormatShort().format(Date())
        server.enqueue(getMediaList("", "", "", 1))

        val media = testObject.getMedia(template, true)!!.blockingGet()

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("json", body["format"])
                Assert.assertEquals("2", body["formatversion"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals(template, body["titles"])
                Assert.assertEquals("images", body["generator"])
                Assert.assertEquals("imageinfo", body["prop"])
                Assert.assertEquals("url|extmetadata", body["iiprop"])
                Assert.assertEquals("DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl", body["iiextmetadatafilter"])
            }
        }

        assert(media is Media)
    }

    /**
     * Test response for getting picture of the day
     */
    @Test
    fun getPictureOfTheDay() {
        val template = "Template:Potd/" + CommonsDateUtil.getIso8601DateFormatShort().format(Date())
        server.enqueue(getMediaList("", "", "", 1))

        val media = testObject.pictureOfTheDay?.blockingGet()

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("json", body["format"])
                Assert.assertEquals("2", body["formatversion"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals(template, body["titles"])
                Assert.assertEquals("images", body["generator"])
                Assert.assertEquals("imageinfo", body["prop"])
                Assert.assertEquals("url|extmetadata", body["iiprop"])
                Assert.assertEquals("DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl", body["iiextmetadatafilter"])
            }
        }

        assert(media is Media)
    }

    private fun testFirstPageSearchQuery() {
        val categoryImages = testObject.getMediaList("search", "Watercraft moored off shore")!!.blockingGet()

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("json", body["format"])
                Assert.assertEquals("2", body["formatversion"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("search", body["generator"])
                Assert.assertEquals("text", body["gsrwhat"])
                Assert.assertEquals("6", body["gsrnamespace"])
                Assert.assertEquals("25", body["gsrlimit"])
                Assert.assertEquals("Watercraft moored off shore", body["gsrsearch"])
                Assert.assertEquals("imageinfo", body["prop"])
                Assert.assertEquals("url|extmetadata", body["iiprop"])
                Assert.assertEquals("DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl", body["iiextmetadatafilter"])
            }
        }
        assertEquals(categoryImages.size, 2)
    }

    private fun testFirstPageQuery() {
        val categoryImages = testObject.getMediaList("category", "Watercraft moored off shore")?.blockingGet()

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("json", body["format"])
                Assert.assertEquals("2", body["formatversion"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("categorymembers", body["generator"])
                Assert.assertEquals("file", body["gcmtype"])
                Assert.assertEquals("Watercraft moored off shore", body["gcmtitle"])
                Assert.assertEquals("timestamp", body["gcmsort"])
                Assert.assertEquals("desc", body["gcmdir"])
                Assert.assertEquals("imageinfo", body["prop"])
                Assert.assertEquals("url|extmetadata", body["iiprop"])
                Assert.assertEquals("DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl", body["iiextmetadatafilter"])
            }
        }
        assertEquals(categoryImages?.size, 2)
    }

    private fun getFirstPageOfImages(): MockResponse {
        return getMediaList("gcmcontinue", "testvalue", "gcmcontinue||", 2)
    }

    private fun getSecondPageOfImages(): MockResponse {
        return getMediaList("gcmcontinue", "testvalue2", "gcmcontinue||", 2)
    }

    private fun getFirstPageOfSearchImages(): MockResponse {
        return getMediaList("gsroffset", "25", "gsroffset||", 2)
    }

    private fun getSecondPageOfSearchImages(): MockResponse {
        return getMediaList("gsroffset", "25", "gsroffset||", 2)
    }

    /**
     * Generate a MockResponse object which contains a list of media pages
     */
    private fun getMediaList(queryContinueType: String,
                             queryContinueValue: String,
                             continueVal: String,
                             numberOfPages: Int): MockResponse {
        val mockResponse = MockResponse()
        mockResponse.setResponseCode(200)
        var continueJson = ""

        if (queryContinueType != "" && queryContinueValue != "" && continueVal != "") {
            continueJson = ",\"continue\":{\"$queryContinueType\":\"$queryContinueValue\",\"continue\":\"$continueVal\"}"
        }

        val mediaList = mutableListOf<String>()
        val random = Random(1000)
        for (i in 0 until numberOfPages) {
            mediaList.add(getMediaPage(random))
        }

        val pagesString = mediaList.joinToString()
        val responseBody = "{\"batchcomplete\":\"\"$continueJson,\"query\":{\"pages\":[$pagesString]}}"
        mockResponse.setBody(responseBody)
        return mockResponse
    }

    /**
     * Generate test media json object
     */
    private fun getMediaPage(random: Random): String {
        val pageID = random.nextInt()
        val id = random.nextInt()
        val fileName = "Test$id"
        val id1 = random.nextInt()
        val id2 = random.nextInt()
        val categories = "cat$id1|cat$id2"
        return "{\"pageid\":$pageID,\"ns\":6,\"title\":\"File:$fileName\",\"imagerepository\":\"local\",\"imageinfo\":[{\"url\":\"https://upload.wikimedia.org/$fileName\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:$fileName\",\"descriptionshorturl\":\"https://commons.wikimedia.org/w/index.php?curid=4406048\",\"extmetadata\":{\"DateTime\":{\"value\":\"2013-04-13 15:12:11\",\"source\":\"mediawiki-metadata\",\"hidden\":\"\"},\"Categories\":{\"value\":\"$categories\",\"source\":\"commons-categories\",\"hidden\":\"\"},\"Artist\":{\"value\":\"<bdi><a href=\\\"https://en.wikipedia.org/wiki/en:Raphael\\\" class=\\\"extiw\\\" title=\\\"w:en:Raphael\\\">Raphael</a>\\n</bdi>\",\"source\":\"commons-desc-page\"},\"ImageDescription\":{\"value\":\"test desc\",\"source\":\"commons-desc-page\"},\"DateTimeOriginal\":{\"value\":\"1511<div style=\\\"display: none;\\\">date QS:P571,+1511-00-00T00:00:00Z/9</div>\",\"source\":\"commons-desc-page\"},\"LicenseShortName\":{\"value\":\"Public domain\",\"source\":\"commons-desc-page\",\"hidden\":\"\"}}}]}"
    }

    /**
     * Check request params
     */
    private fun assertBasicRequestParameters(server: MockWebServer, method: String): RecordedRequest = server.takeRequest().let {
        Assert.assertEquals("/", it.requestUrl.encodedPath())
        Assert.assertEquals(method, it.method)
        return it
    }


    /**
     * Parse query params
     */
    private fun parseQueryParams(request: RecordedRequest) = HashMap<String, String?>().apply {
        request.requestUrl.let {
            it.queryParameterNames().forEach { name -> put(name, it.queryParameter(name)) }
        }
    }

}