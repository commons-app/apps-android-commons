package fr.free.nrw.commons.leaderboard

import com.google.gson.Gson
import fr.free.nrw.commons.profile.leaderboard.LeaderboardResponse
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * This class tests the Leaderboard API calls
 */
class LeaderboardApiTest {
    lateinit var server: MockWebServer

    /**
     * This method initialises a Mock Server
     */
    @Before
    fun initTest() {
        server = MockWebServer()
    }

    /**
     * This method will setup a Mock Server and load Test JSON Response File
     * @throws Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val testResponseBody = convertStreamToString(
            javaClass.classLoader!!.getResourceAsStream(FILE_NAME)
        )

        server.enqueue(MockResponse().setBody(testResponseBody))
        server.start()
    }

    /**
     * This method will call the Mock Server and Test it with sample values.
     * It will test the Leaderboard API call functionality and check if the object is
     * being created with the correct values
     * @throws IOException
     */
    @Test
    @Throws(IOException::class)
    fun apiTest() {
        val httpUrl = server.url(ENDPOINT)
        val response = sendRequest(OkHttpClient(), httpUrl)

        Assert.assertEquals(TEST_AVATAR, response!!.avatar)
        Assert.assertEquals(TEST_USERNAME, response.username)
        Assert.assertEquals(TEST_USER_RANK, response.rank)
        Assert.assertEquals(TEST_USER_COUNT, response.categoryCount)
    }

    /**
     * This method will call the Mock API and returns the Leaderboard Response Object
     * @param okHttpClient
     * @param httpUrl
     * @return Leaderboard Response Object
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun sendRequest(okHttpClient: OkHttpClient, httpUrl: HttpUrl): LeaderboardResponse? {
        val request: Request = Request.Builder().url(httpUrl).build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val gson = Gson()
            return gson.fromJson(response.body!!.string(), LeaderboardResponse::class.java)
        }
        return null
    }

    /**
     * This method shuts down the Mock Server
     * @throws IOException
     */
    @After
    @Throws(IOException::class)
    fun shutdown() {
        server.shutdown()
    }

    companion object {
        private const val TEST_USERNAME = "user"
        private const val TEST_AVATAR = "avatar"
        private const val TEST_USER_RANK = 1
        private const val TEST_USER_COUNT = 0

        private const val FILE_NAME = "leaderboard_sample_response.json"
        private const val ENDPOINT = "/leaderboard.py"

        /**
         * This method converts a Input Stream to String
         * @param is takes Input Stream of JSON File as Parameter
         * @return a String with JSON data
         * @throws Exception
         */
        @Throws(Exception::class)
        private fun convertStreamToString(`is`: InputStream): String {
            val reader = BufferedReader(InputStreamReader(`is`))
            val sb = StringBuilder()
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                sb.append(line).append("\n")
            }
            reader.close()
            return sb.toString()
        }
    }
}
