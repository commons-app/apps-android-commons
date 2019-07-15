package fr.free.nrw.commons.mwapi

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.ConfigUtils
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wikipedia.util.DateUtil
import java.net.URLDecoder
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ApacheHttpClientMediaWikiApiTest {

    private lateinit var testObject: ApacheHttpClientMediaWikiApi
    private lateinit var server: MockWebServer
    private lateinit var wikidataServer: MockWebServer
    private lateinit var sharedPreferences: JsonKvStore
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        wikidataServer = MockWebServer()
        okHttpClient = OkHttpClient()
        sharedPreferences = mock(JsonKvStore::class.java)
        testObject = ApacheHttpClientMediaWikiApi("http://" + server.hostName + ":" + server.port + "/")
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun isUserBlockedFromCommonsForInfinitelyBlockedUser() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"infinite\"></userinfo></query></api>"))

        val result = testObject.isUserBlockedFromCommons()

        assertBasicRequestParameters(server, "GET").let { userBlockedRequest ->
            parseQueryParams(userBlockedRequest).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("userinfo", body["meta"])
                assertEquals("blockinfo", body["uiprop"])
            }
        }

        assertTrue(result)
    }

    @Test
    fun isUserBlockedFromCommonsForTimeBlockedUser() {
        val currentDate = Date()
        val expiredDate = Date(currentDate.time + 10000)
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"" + DateUtil.iso8601DateFormat(expiredDate) + "\"></userinfo></query></api>"))

        val result = testObject.isUserBlockedFromCommons()

        assertBasicRequestParameters(server, "GET").let { userBlockedRequest ->
            parseQueryParams(userBlockedRequest).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("userinfo", body["meta"])
                assertEquals("blockinfo", body["uiprop"])
            }
        }

        assertTrue(result)
    }

    @Test
    fun isUserBlockedFromCommonsForExpiredBlockedUser() {
        val currentDate = Date()
        val expiredDate = Date(currentDate.time - 10000)
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"" + DateUtil.iso8601DateFormat(expiredDate) + "\"></userinfo></query></api>"))

        val result = testObject.isUserBlockedFromCommons()

        assertBasicRequestParameters(server, "GET").let { userBlockedRequest ->
            parseQueryParams(userBlockedRequest).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("userinfo", body["meta"])
                assertEquals("blockinfo", body["uiprop"])
            }
        }

        assertFalse(result)
    }

    @Test
    fun isUserBlockedFromCommonsForNotBlockedUser() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\"></userinfo></query></api>"))

        val result = testObject.isUserBlockedFromCommons()

        assertBasicRequestParameters(server, "GET").let { userBlockedRequest ->
            parseQueryParams(userBlockedRequest).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("userinfo", body["meta"])
                assertEquals("blockinfo", body["uiprop"])
            }
        }

        assertFalse(result)
    }

    private fun assertBasicRequestParameters(server: MockWebServer, method: String): RecordedRequest = server.takeRequest().let {
        assertEquals("/", it.requestUrl.encodedPath())
        assertEquals(method, it.method)
        assertEquals("Commons/${ConfigUtils.getVersionNameWithSha(ApplicationProvider.getApplicationContext())} (https://mediawiki.org/wiki/Apps/Commons) Android/${Build.VERSION.RELEASE}",
                it.getHeader("User-Agent"))
        if ("POST" == method) {
            assertEquals("application/x-www-form-urlencoded", it.getHeader("Content-Type"))
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
