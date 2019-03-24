package fr.free.nrw.commons.mwapi

import android.os.Build
import com.google.gson.Gson
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.ConfigUtils
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.URLDecoder
import java.util.*

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

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun getCategoryImages() {
        server.enqueue(getFirstPageOfImages())
        val categoryImages = testObject.getCategoryImages("Watercraft moored off shore")

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("format", body["json"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("generator", body["categorymembers"])
                Assert.assertEquals("gcmtype", body["file"])
                Assert.assertEquals("gcmtitle", body["Watercraft moored off shore"])
                Assert.assertEquals("gcmsort", body["timestamp"])
                Assert.assertEquals("gcmdir", body["desc"])
                Assert.assertEquals("gcmlimit", body["2"])
            }
        }
        assertEquals(categoryImages!!.blockingGet().size, 2)
    }

    private fun getFirstPageOfImages(): MockResponse {
        val mockResponse = MockResponse()
        mockResponse.setResponseCode(200)
        mockResponse.setBody("{\"batchcomplete\":\"\",\"continue\":{\"gcmcontinue\":\"testvalue\",\"continue\":\"gcmcontinue||\"},\"query\":{\"pages\":{\"4406048\":{\"pageid\":4406048,\"ns\":6,\"title\":\"File:test1.jpg\",\"imagerepository\":\"local\",\"imageinfo\":[{\"url\":\"https://upload.wikimedia.org/test1.jpg\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:test1.jpg\",\"descriptionshorturl\":\"https://commons.wikimedia.org/w/index.php?curid=4406048\",\"extmetadata\":{\"DateTime\":{\"value\":\"2013-04-13 15:12:11\",\"source\":\"mediawiki-metadata\",\"hidden\":\"\"},\"Categories\":{\"value\":\"cat1|cat2\",\"source\":\"commons-categories\",\"hidden\":\"\"},\"Artist\":{\"value\":\"<bdi><a href=\\\"https://en.wikipedia.org/wiki/en:Raphael\\\" class=\\\"extiw\\\" title=\\\"w:en:Raphael\\\">Raphael</a>\\n</bdi>\",\"source\":\"commons-desc-page\"},\"ImageDescription\":{\"value\":\"test desc\",\"source\":\"commons-desc-page\"},\"DateTimeOriginal\":{\"value\":\"1511<div style=\\\"display: none;\\\">date QS:P571,+1511-00-00T00:00:00Z/9</div>\",\"source\":\"commons-desc-page\"},\"LicenseShortName\":{\"value\":\"Public domain\",\"source\":\"commons-desc-page\",\"hidden\":\"\"}}}]},\"24259710\":{\"pageid\":24259710,\"ns\":6,\"title\":\"File:test2.jpg\",\"imagerepository\":\"local\",\"imageinfo\":[{\"url\":\"https://upload.wikimedia.org/test2.jpg\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:test2.jpg\",\"descriptionshorturl\":\"https://commons.wikimedia.org/w/index.php?curid=4406048\",\"extmetadata\":{\"DateTime\":{\"value\":\"2013-04-13 15:12:11\",\"source\":\"mediawiki-metadata\",\"hidden\":\"\"},\"Categories\":{\"value\":\"cat3|cat4\",\"source\":\"commons-categories\",\"hidden\":\"\"},\"Artist\":{\"value\":\"<bdi><a href=\\\"https://en.wikipedia.org/wiki/en:Raphael\\\" class=\\\"extiw\\\" title=\\\"w:en:Raphael\\\">Raphael</a>\\n</bdi>\",\"source\":\"commons-desc-page\"},\"ImageDescription\":{\"value\":\"test desc\",\"source\":\"commons-desc-page\"},\"DateTimeOriginal\":{\"value\":\"1511<div style=\\\"display: none;\\\">date QS:P571,+1511-00-00T00:00:00Z/9</div>\",\"source\":\"commons-desc-page\"},\"LicenseShortName\":{\"value\":\"Public domain\",\"source\":\"commons-desc-page\",\"hidden\":\"\"}}}]}}}}")
        return mockResponse
    }

    private fun getSecondPageOfImages(): MockResponse {
        val mockResponse = MockResponse()
        mockResponse.setResponseCode(200)
        mockResponse.setBody("{\"batchcomplete\":\"\",\"continue\":{\"gcmcontinue\":\"testvalue2\",\"continue\":\"gcmcontinue||\"},\"query\":{\"pages\":{\"4406048\":{\"pageid\":4406048,\"ns\":6,\"title\":\"File:test3.jpg\",\"imagerepository\":\"local\",\"imageinfo\":[{\"url\":\"https://upload.wikimedia.org/test3.jpg\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:test3.jpg\",\"descriptionshorturl\":\"https://commons.wikimedia.org/w/index.php?curid=4406048\",\"extmetadata\":{\"DateTime\":{\"value\":\"2013-04-13 15:12:11\",\"source\":\"mediawiki-metadata\",\"hidden\":\"\"},\"Categories\":{\"value\":\"cat5|cat6\",\"source\":\"commons-categories\",\"hidden\":\"\"},\"Artist\":{\"value\":\"<bdi><a href=\\\"https://en.wikipedia.org/wiki/en:Raphael\\\" class=\\\"extiw\\\" title=\\\"w:en:Raphael\\\">Raphael</a>\\n</bdi>\",\"source\":\"commons-desc-page\"},\"ImageDescription\":{\"value\":\"test desc\",\"source\":\"commons-desc-page\"},\"DateTimeOriginal\":{\"value\":\"1511<div style=\\\"display: none;\\\">date QS:P571,+1511-00-00T00:00:00Z/9</div>\",\"source\":\"commons-desc-page\"},\"LicenseShortName\":{\"value\":\"Public domain\",\"source\":\"commons-desc-page\",\"hidden\":\"\"}}}]},\"24259710\":{\"pageid\":24259710,\"ns\":6,\"title\":\"File:test4.jpg\",\"imagerepository\":\"local\",\"imageinfo\":[{\"url\":\"https://upload.wikimedia.org/test4.jpg\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:test4.jpg\",\"descriptionshorturl\":\"https://commons.wikimedia.org/w/index.php?curid=4406048\",\"extmetadata\":{\"DateTime\":{\"value\":\"2013-04-13 15:12:11\",\"source\":\"mediawiki-metadata\",\"hidden\":\"\"},\"Categories\":{\"value\":\"cat7\",\"source\":\"commons-categories\",\"hidden\":\"\"},\"Artist\":{\"value\":\"<bdi><a href=\\\"https://en.wikipedia.org/wiki/en:Raphael\\\" class=\\\"extiw\\\" title=\\\"w:en:Raphael\\\">Raphael</a>\\n</bdi>\",\"source\":\"commons-desc-page\"},\"ImageDescription\":{\"value\":\"test desc\",\"source\":\"commons-desc-page\"},\"DateTimeOriginal\":{\"value\":\"1511<div style=\\\"display: none;\\\">date QS:P571,+1511-00-00T00:00:00Z/9</div>\",\"source\":\"commons-desc-page\"},\"LicenseShortName\":{\"value\":\"Public domain\",\"source\":\"commons-desc-page\",\"hidden\":\"\"}}}]}}}}")
        return mockResponse
    }

    @Test
    fun getCategoryImagesWithContinue() {
        server.enqueue(getFirstPageOfImages())
        server.enqueue(getSecondPageOfImages())
        val categoryImages = testObject.getCategoryImages("Watercraft moored off shore")

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("format", body["json"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("generator", body["categorymembers"])
                Assert.assertEquals("gcmtype", body["file"])
                Assert.assertEquals("gcmtitle", body["Watercraft moored off shore"])
                Assert.assertEquals("gcmsort", body["timestamp"])
                Assert.assertEquals("gcmdir", body["desc"])
                Assert.assertEquals("gcmlimit", body["2"])
            }
        }

        assertEquals(categoryImages!!.blockingGet().size, 2)

        val categoryImagesContinued = testObject.getCategoryImages("Watercraft moored off shore")

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { body ->
                Assert.assertEquals("format", body["json"])
                Assert.assertEquals("query", body["action"])
                Assert.assertEquals("generator", body["categorymembers"])
                Assert.assertEquals("gcmtype", body["file"])
                Assert.assertEquals("gcmtitle", body["Watercraft moored off shore"])
                Assert.assertEquals("gcmsort", body["timestamp"])
                Assert.assertEquals("gcmdir", body["desc"])
                Assert.assertEquals("gcmlimit", body["2"])
                Assert.assertEquals("gcmcontinue", body["testvalue"])
                Assert.assertEquals("continue", body["gcmcontinue||"])
            }
        }

        assertEquals(categoryImagesContinued!!.blockingGet().size, 2)
    }

    private fun assertBasicRequestParameters(server: MockWebServer, method: String): RecordedRequest = server.takeRequest().let {
        Assert.assertEquals("/", it.requestUrl.encodedPath())
        Assert.assertEquals(method, it.method)
        Assert.assertEquals("Commons/${ConfigUtils.getVersionNameWithSha(RuntimeEnvironment.application)} (https://mediawiki.org/wiki/Apps/Commons) Android/${Build.VERSION.RELEASE}",
                it.getHeader("User-Agent"))
        if ("POST" == method) {
            Assert.assertEquals("application/x-www-form-urlencoded", it.getHeader("Content-Type"))
        }
        return it
    }

    private fun parseQueryParams(request: RecordedRequest) = HashMap<String, String?>().apply {
        request.requestUrl.let {
            it.queryParameterNames().forEach { name -> put(name, it.queryParameter(name)) }
        }
    }

    private fun parseBody(body: String): Map<String, String> = HashMap<String, String>().apply {
        body.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().forEach { prop ->
            val pair = prop.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            put(pair[0], URLDecoder.decode(pair[1], "utf-8"))
        }
    }
}