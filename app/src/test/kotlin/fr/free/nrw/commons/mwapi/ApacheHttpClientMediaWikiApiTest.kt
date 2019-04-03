package fr.free.nrw.commons.mwapi

import android.os.Build
import com.google.gson.Gson
import fr.free.nrw.commons.BuildConfig
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wikipedia.util.DateUtil
import java.net.URLDecoder
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(21), application = TestCommonsApplication::class)
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
        testObject = ApacheHttpClientMediaWikiApi(RuntimeEnvironment.application, "http://" + server.hostName + ":" + server.port + "/", "http://" + wikidataServer.hostName + ":" + wikidataServer.port + "/", sharedPreferences, Gson())
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun authCookiesAreHandled() {
        assertEquals("", testObject.authCookie)

        testObject.authCookie = "cookie=chocolate-chip"

        assertEquals("cookie=chocolate-chip", testObject.authCookie)
    }

    @Test
    fun simpleLoginWithWrongPassword() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api batchcomplete=\"\"><query><tokens logintoken=\"baz\" /></query></api>"))
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><clientlogin status=\"FAIL\" message=\"Incorrect password entered.&#10;Please try again.\" messagecode=\"wrongpassword\" /></api>"))

        val result = testObject.login("foo", "bar")

        assertBasicRequestParameters(server, "POST").let { loginTokenRequest ->
            parseBody(loginTokenRequest.body.readUtf8()).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("login", body["type"])
                assertEquals("tokens", body["meta"])
            }
        }

        assertBasicRequestParameters(server, "POST").let { loginRequest ->
            parseBody(loginRequest.body.readUtf8()).let { body ->
                assertEquals("1", body["rememberMe"])
                assertEquals("foo", body["username"])
                assertEquals("bar", body["password"])
                assertEquals("baz", body["logintoken"])
                assertEquals("https://commons.wikimedia.org", body["loginreturnurl"])
                assertEquals("xml", body["format"])
            }
        }

        assertEquals("wrongpassword", result)
    }

    @Test
    fun simpleLogin() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api batchcomplete=\"\"><query><tokens logintoken=\"baz\" /></query></api>"))
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><clientlogin status=\"PASS\" username=\"foo\" /></api>"))

        val result = testObject.login("foo", "bar")

        assertBasicRequestParameters(server, "POST").let { loginTokenRequest ->
            parseBody(loginTokenRequest.body.readUtf8()).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("login", body["type"])
                assertEquals("tokens", body["meta"])
            }
        }

        assertBasicRequestParameters(server, "POST").let { loginRequest ->
            parseBody(loginRequest.body.readUtf8()).let { body ->
                assertEquals("1", body["rememberMe"])
                assertEquals("foo", body["username"])
                assertEquals("bar", body["password"])
                assertEquals("baz", body["logintoken"])
                assertEquals("https://commons.wikimedia.org", body["loginreturnurl"])
                assertEquals("xml", body["format"])
            }
        }

        assertEquals("PASS", result)
    }

    @Test
    fun twoFactorLogin() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api batchcomplete=\"\"><query><tokens logintoken=\"baz\" /></query></api>"))
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><clientlogin status=\"PASS\" username=\"foo\" /></api>"))

        val result = testObject.login("foo", "bar", "2fa")

        assertBasicRequestParameters(server, "POST").let { loginTokenRequest ->
            parseBody(loginTokenRequest.body.readUtf8()).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("login", body["type"])
                assertEquals("tokens", body["meta"])
            }
        }

        assertBasicRequestParameters(server, "POST").let { loginRequest ->
            parseBody(loginRequest.body.readUtf8()).let { body ->
                assertEquals("true", body["rememberMe"])
                assertEquals("foo", body["username"])
                assertEquals("bar", body["password"])
                assertEquals("baz", body["logintoken"])
                assertEquals("true", body["logincontinue"])
                assertEquals("2fa", body["OATHToken"])
                assertEquals("xml", body["format"])
            }
        }

        assertEquals("PASS", result)
    }

    @Test
    fun validateLoginForLoggedInUser() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"10\" name=\"foo\"/></query></api>"))

        val result = testObject.validateLogin()

        assertBasicRequestParameters(server, "GET").let { loginTokenRequest ->
            parseQueryParams(loginTokenRequest).let { body ->
                assertEquals("xml", body["format"])
                assertEquals("query", body["action"])
                assertEquals("userinfo", body["meta"])
            }
        }

        assertTrue(result)
    }

    @Test
    fun validateLoginForLoggedOutUser() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"0\" name=\"foo\"/></query></api>"))

        val result = testObject.validateLogin()

        assertBasicRequestParameters(server, "GET").let { loginTokenRequest ->
            parseQueryParams(loginTokenRequest).let { params ->
                assertEquals("xml", params["format"])
                assertEquals("query", params["action"])
                assertEquals("userinfo", params["meta"])
            }
        }

        assertFalse(result)
    }

    @Test
    fun editToken() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><tokens csrftoken=\"baz\" /></query></api>"))

        val result = testObject.editToken

        assertBasicRequestParameters(server, "POST").let { editTokenRequest ->
            parseBody(editTokenRequest.body.readUtf8()).let { body ->
                assertEquals("query", body["action"])
                assertEquals("tokens", body["meta"])
            }
        }

        assertEquals("baz", result)
    }

    @Test
    fun getWikidataEditToken() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><centralauthtoken centralauthtoken=\"abc\" /></api>"))
        wikidataServer.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><tokens csrftoken=\"baz\" /></query></api>"))

        val result = testObject.wikidataCsrfToken

        assertBasicRequestParameters(server, "GET").let { centralAuthTokenRequest ->
            parseQueryParams(centralAuthTokenRequest).let { params ->
                assertEquals("xml", params["format"])
                assertEquals("centralauthtoken", params["action"])
            }
        }

        assertBasicRequestParameters(wikidataServer, "POST").let { editTokenRequest ->
            parseBody(editTokenRequest.body.readUtf8()).let { body ->
                assertEquals("query", body["action"])
                assertEquals("abc", body["centralauthtoken"])
                assertEquals("tokens", body["meta"])
            }
        }

        assertEquals("baz", result)
    }

    @Test
    fun fileExistsWithName_FileNotFound() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api batchcomplete=\"\"><query> <normalized><n from=\"File:foo\" to=\"File:Foo\" /></normalized><pages><page _idx=\"-1\" ns=\"6\" title=\"File:Foo\" missing=\"\" imagerepository=\"\" /></pages></query></api>"))

        val result = testObject.fileExistsWithName("foo")

        assertBasicRequestParameters(server, "GET").let { request ->
            parseQueryParams(request).let { params ->
                assertEquals("xml", params["format"])
                assertEquals("query", params["action"])
                assertEquals("imageinfo", params["prop"])
                assertEquals("File:foo", params["titles"])
            }
        }

        assertFalse(result)
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
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"" + DateUtil.getIso8601DateFormat().format(expiredDate) + "\"></userinfo></query></api>"))

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
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"" + DateUtil.getIso8601DateFormat().format(expiredDate) + "\"></userinfo></query></api>"))

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
        assertEquals("Commons/${ConfigUtils.getVersionNameWithSha(RuntimeEnvironment.application)} (https://mediawiki.org/wiki/Apps/Commons) Android/${Build.VERSION.RELEASE}",
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
