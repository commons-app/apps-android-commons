package fr.free.nrw.commons.mwapi

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import com.google.gson.Gson
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
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
import timber.log.Timber
import java.io.InputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(21), application = TestCommonsApplication::class)
class ApacheHttpClientMediaWikiApiTest {

    private lateinit var testObject: ApacheHttpClientMediaWikiApi
    private lateinit var server: MockWebServer
    private lateinit var wikidataServer: MockWebServer
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var categoryPreferences: SharedPreferences
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        wikidataServer = MockWebServer()
        okHttpClient = OkHttpClient()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
        categoryPreferences = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
        testObject = ApacheHttpClientMediaWikiApi(RuntimeEnvironment.application, "http://" + server.hostName + ":" + server.port + "/", "http://" + wikidataServer.hostName + ":" + wikidataServer.port + "/", sharedPreferences, categoryPreferences, Gson(), okHttpClient)
        testObject.setWikiMediaToolforgeUrl("http://" + server.hostName + ":" + server.port + "/")
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
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><centralauthtoken centralauthtoken=\"abc\" /></api>"))
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><tokens csrftoken=\"baz\" /></query></api>"))

        val result = testObject.editToken

        assertBasicRequestParameters(server, "GET").let { centralAuthTokenRequest ->
            parseQueryParams(centralAuthTokenRequest).let { params ->
                assertEquals("xml", params["format"])
                assertEquals("centralauthtoken", params["action"])
            }
        }

        assertBasicRequestParameters(server, "POST").let { editTokenRequest ->
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

//    @Test
//    fun uploadFile() {
//        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><centralauthtoken centralauthtoken=\"abc\" /></api>"))
//        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><centralauthtoken centralauthtoken=\"abc\" /></api>"))
//        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><tokens csrftoken=\"baz\" /></query></api>"))
//
//
//        server.enqueue(MockResponse()
//                .setBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?><api><upload result=\"Success\" filename=\"Test_image_for_response.jpg\"><imageinfo timestamp=\"2018-12-07T15:14:44Z\" user=\"Maskaravivek\" userid=\"1725\" size=\"198315\" width=\"960\" height=\"1280\" parsedcomment=\"Uploaded using &lt;a href=&quot;/w/index.php?title=Commons:MOA&amp;amp;action=edit&amp;amp;redlink=1&quot; class=&quot;new&quot; title=&quot;Commons:MOA (page does not exist)&quot;&gt;Commons Mobile App&lt;/a&gt;\" comment=\"Uploaded using [[COM:MOA|Commons Mobile App]]\" html=\"&lt;div&gt;&#10;&lt;div class=&quot;thumb tright&quot;&gt;&lt;div class=&quot;thumbinner&quot; style=&quot;width:182px;&quot;&gt;&lt;a href=&quot;/w/index.php?title=Special:Upload&amp;amp;wpDestFile=Test_image_for_response.jpg&quot; class=&quot;new&quot; title=&quot;File:Test image for response.jpg&quot;&gt;File:Test image for response.jpg&lt;/a&gt;  &lt;div class=&quot;thumbcaption&quot;&gt;Existing file&lt;/div&gt;&lt;/div&gt;&lt;/div&gt;&#10;&lt;p&gt;&lt;span id=&quot;wpUploadWarningFileexists&quot;&gt;A file with this name already exists; please check &lt;b&gt;&lt;a class=&quot;mw-selflink selflink&quot;&gt;the existing file&lt;/a&gt;&lt;/b&gt; if you are not sure whether you want to change it. Please choose another filename, unless you are uploading a technically improved version of the same file. &lt;br /&gt;Do not overwrite an image with a different one of the same topic (see &lt;a href=&quot;/w/index.php?title=Commons:File_naming&amp;amp;action=edit&amp;amp;redlink=1&quot; class=&quot;new&quot; title=&quot;Commons:File naming (page does not exist)&quot;&gt;file naming&lt;/a&gt;).&lt;/span&gt;&#10;&lt;/p&gt;&#10;&lt;div style=&quot;clear:both;&quot;&gt;&lt;/div&gt;&#10;&lt;/div&gt;&#10;\" canonicaltitle=\"File:Test image for response.jpg\" url=\"https://upload.beta.wmflabs.org/wikipedia/commons/f/f1/Test_image_for_response.jpg\" descriptionurl=\"https://commons.wikimedia.beta.wmflabs.org/wiki/File:Test_image_for_response.jpg\" descriptionshorturl=\"https://commons.wikimedia.beta.wmflabs.org/w/index.php?curid=0\" sha1=\"46a6aa719f006e1ab9d810ac2104ec6e66352010\" mime=\"image/jpeg\" mediatype=\"BITMAP\" bitdepth=\"8\"><metadata><metadata name=\"Model\" value=\"Android SDK built for x86\"/><metadata name=\"ImageWidth\" value=\"960\"/><metadata name=\"ImageLength\" value=\"1280\"/><metadata name=\"DateTime\" value=\"2018:11:30 24:34:55\"/><metadata name=\"Orientation\" value=\"1\"/><metadata name=\"Make\" value=\"Google\"/><metadata name=\"FNumber\" value=\"280/100\"/><metadata name=\"ExposureTime\" value=\"9993662/1000000000\"/><metadata name=\"SubSecTimeDigitized\" value=\"010\"/><metadata name=\"SubSecTimeOriginal\" value=\"010\"/><metadata name=\"SubSecTime\" value=\"010\"/><metadata name=\"FocalLength\" value=\"5000/1000\"/><metadata name=\"Flash\" value=\"0\"/><metadata name=\"ISOSpeedRatings\" value=\"100\"/><metadata name=\"DateTimeDigitized\" value=\"2018:11:30 24:34:55\"/><metadata name=\"DateTimeOriginal\" value=\"2018:11:30 24:34:55\"/><metadata name=\"WhiteBalance\" value=\"0\"/><metadata name=\"ApertureValue\" value=\"297/100\"/><metadata name=\"ShutterSpeedValue\" value=\"6644/1000\"/><metadata name=\"ExifVersion\" value=\"0220\"/><metadata name=\"GPSLatitude\" value=\"37.421997222222\"/><metadata name=\"GPSLongitude\" value=\"-122.084\"/><metadata name=\"GPSAltitude\" value=\"5/1\"/><metadata name=\"GPSTimeStamp\"><value><metadata name=\"0\" value=\"19/1\"/><metadata name=\"1\" value=\"4/1\"/><metadata name=\"2\" value=\"55/1\"/></value></metadata><metadata name=\"GPSDateStamp\" value=\"2018:11:29\"/><metadata name=\"MEDIAWIKI_EXIF_VERSION\" value=\"2\"/></metadata><commonmetadata><metadata name=\"Model\" value=\"Android SDK built for x86\"/><metadata name=\"ImageWidth\" value=\"960\"/><metadata name=\"ImageLength\" value=\"1280\"/><metadata name=\"DateTime\" value=\"2018:11:30 24:34:55\"/><metadata name=\"Orientation\" value=\"1\"/><metadata name=\"Make\" value=\"Google\"/><metadata name=\"FNumber\" value=\"280/100\"/><metadata name=\"ExposureTime\" value=\"9993662/1000000000\"/><metadata name=\"SubSecTimeDigitized\" value=\"010\"/><metadata name=\"SubSecTimeOriginal\" value=\"010\"/><metadata name=\"SubSecTime\" value=\"010\"/><metadata name=\"FocalLength\" value=\"5000/1000\"/><metadata name=\"Flash\" value=\"0\"/><metadata name=\"ISOSpeedRatings\" value=\"100\"/><metadata name=\"DateTimeDigitized\" value=\"2018:11:30 24:34:55\"/><metadata name=\"DateTimeOriginal\" value=\"2018:11:30 24:34:55\"/><metadata name=\"WhiteBalance\" value=\"0\"/><metadata name=\"ApertureValue\" value=\"297/100\"/><metadata name=\"ShutterSpeedValue\" value=\"6644/1000\"/><metadata name=\"ExifVersion\" value=\"0220\"/><metadata name=\"GPSLatitude\" value=\"37.421997222222\"/><metadata name=\"GPSLongitude\" value=\"-122.084\"/><metadata name=\"GPSAltitude\" value=\"5/1\"/><metadata name=\"GPSTimeStamp\"><value><metadata name=\"0\" value=\"19/1\"/><metadata name=\"1\" value=\"4/1\"/><metadata name=\"2\" value=\"55/1\"/></value></metadata><metadata name=\"GPSDateStamp\" value=\"2018:11:29\"/></commonmetadata><extmetadata><DateTime value=\"2018-12-07 15:14:44\" source=\"mediawiki-metadata\" hidden=\"\"/><ObjectName value=\"Test image for response\" source=\"mediawiki-metadata\" hidden=\"\"/><CommonsMetadataExtension value=\"1.2\" source=\"extension\" hidden=\"\"/><Categories value=\"\" source=\"commons-categories\" hidden=\"\"/><Assessments value=\"\" source=\"commons-categories\" hidden=\"\"/></extmetadata></imageinfo></upload></api>")
//                .setResponseCode(200))
//
//        val listener: MediaWikiApi.ProgressListener = MediaWikiApi.ProgressListener { transferred, total ->
//            Timber.d("Transferred %d of total %d", transferred, total)
//        }
//        testObject.uploadFile("test.jpg",
//                mock(InputStream::class.java),
//                1,
//                "test",
//                "test",
//                Uri.parse("test"),
//                Uri.parse("test"),
//                listener)
//    }

    @Test
    fun getUploadCount() {
        server.enqueue(MockResponse().setBody("23\n"))

        val testObserver = testObject.getUploadCount("testUsername").test()

        assertEquals("testUsername", parseQueryParams(server.takeRequest())["user"])
        assertEquals(1, testObserver.valueCount())
        assertEquals(23, testObserver.values()[0])
    }

    @Test
    fun isUserBlockedFromCommonsForInfinitelyBlockedUser() {
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"infinite\"></userinfo></query></api>"))

        val result = testObject.isUserBlockedFromCommons();

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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"" + dateFormat.format(expiredDate) + "\"></userinfo></query></api>"))

        val result = testObject.isUserBlockedFromCommons();

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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        server.enqueue(MockResponse().setBody("<?xml version=\"1.0\"?><api><query><userinfo id=\"1000\" name=\"testusername\" blockid=\"3000\" blockedby=\"blockerusername\" blockedbyid=\"1001\" blockreason=\"testing\" blockedtimestamp=\"2018-05-24T15:32:09Z\" blockexpiry=\"" + dateFormat.format(expiredDate) + "\"></userinfo></query></api>"))

        val result = testObject.isUserBlockedFromCommons();

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

        val result = testObject.isUserBlockedFromCommons();

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
        assertEquals("Commons/${BuildConfig.VERSION_NAME} (https://mediawiki.org/wiki/Apps/Commons) Android/${Build.VERSION.RELEASE}",
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
