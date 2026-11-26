package fr.free.nrw.commons.mwapi

import fr.free.nrw.commons.UnsuccessfulResponseException
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test

/** Checks that a failed location category lookup is reported as an error, not as "no categories". */
class CategoryApiTest {
    @get:Rule
    val server = MockWebServer()

    // CategoryApi always calls the Commons API host, so send its requests to the local server
    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val url =
                    chain
                        .request()
                        .url
                        .newBuilder()
                        .scheme("http")
                        .host(server.hostName)
                        .port(server.port)
                        .build()
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .url(url)
                        .build(),
                )
            }.build()

    private val categoryApi = CategoryApi(client, GsonUtil.defaultGson)

    @Test
    fun returnsTheCategoriesOfNearbyFiles() {
        server.enqueue(
            MockResponse().setBody(
                """{"query":{"pages":[{"title":"File:A.jpg","categories":[{"ns":14,"title":"Category:Delhi"}]}]}}""",
            ),
        )

        categoryApi
            .request("28.6448|77.216721")
            .test()
            .assertResult(listOf(CategoryItem("Delhi", "", "", false)))
    }

    @Test
    fun failsWhenTheApiReturnsAnError() {
        // The API answers errors with HTTP 200, in the format the request asks for (as beta does for maxlag)
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    MockResponse().setBody(
                        if (request.requestUrl?.queryParameter("errorformat") == "plaintext") {
                            """{"errors":[{"code":"maxlag","text":"Waiting for a server: 0 seconds lagged."}]}"""
                        } else {
                            """{"error":{"code":"maxlag","info":"Waiting for a server: 0 seconds lagged."}}"""
                        },
                    )
            }

        categoryApi
            .request("28.6448|77.216721")
            .test()
            .assertNoValues()
            .assertError(MwException::class.java)
    }

    @Test
    fun failsWhenTheServerReturnsAnEmptyErrorResponse() {
        server.enqueue(MockResponse().setResponseCode(503))

        categoryApi
            .request("28.6448|77.216721")
            .test()
            .assertNoValues()
            .assertError(UnsuccessfulResponseException::class.java)
    }

    @Test
    fun failsWhenTheServerIsRateLimiting() {
        server.enqueue(MockResponse().setResponseCode(429).setBody("Too many requests"))

        categoryApi
            .request("28.6448|77.216721")
            .test()
            .assertNoValues()
            .assertError(UnsuccessfulResponseException::class.java)
    }
}
