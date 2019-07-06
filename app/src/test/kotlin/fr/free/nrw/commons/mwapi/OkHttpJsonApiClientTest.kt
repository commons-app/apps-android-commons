package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.CommonsDateUtil
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*
import kotlin.random.Random

/**
 * Mock web server based tests for ok http json api client
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = TestCommonsApplication::class)
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
        testObject = OkHttpJsonApiClient(okHttpClient, HttpUrl.get(toolsForgeUrl), sparqlUrl, campaignsUrl, serverUrl, Gson())
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
     * Check request params with encoded path
     */
    private fun assertBasicRequestParameters(server: MockWebServer, method: String,encodedPath: String): RecordedRequest = server.takeRequest().let {
        Assert.assertEquals(encodedPath, it.requestUrl.encodedPath())
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


    /**
     * Test getUploadCount posititive and negative cases
     */
    @Test
    fun testGetUploadCount(){
        //Positive
        assertEquals(testBaseCasesAndGetUploadCount(true), 20)
        //Negative
        assertEquals(testBaseCasesAndGetUploadCount(false), 0)
    }

    /**
     * Test getUploadCount base cases
     */
    private fun testBaseCasesAndGetUploadCount(shouldAddResponse: Boolean): Int? {
        val mockResponse = MockResponse()
        mockResponse.setResponseCode(200)
        if(shouldAddResponse) {
            val responseBody = "20"
            mockResponse.setBody(responseBody)
        }
        toolsForgeServer.enqueue(mockResponse)

        val uploadCount=testObject.getUploadCount("ashishkumar294").blockingGet()
        assertBasicRequestParameters(toolsForgeServer, "GET","/uploadsbyuser.py").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("ashishkumar294", body["user"])
            }
        }
        return uploadCount
    }

}