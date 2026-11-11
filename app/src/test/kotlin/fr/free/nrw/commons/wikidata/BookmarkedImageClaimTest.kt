package fr.free.nrw.commons.wikidata

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.WikidataPlace
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/** Exercises the Wikidata requests made for repeat uploads from a saved bookmark. */
class BookmarkedImageClaimTest {
    /** A stale bookmark must cause exactly one P18 write across two completed uploads. */
    @Test
    fun secondUploadDoesNotWriteAnotherImageClaim() {
        MockWebServer().use { server ->
            val wikidata = ImageClaimDispatcher()
            server.dispatcher = wikidata
            val gson = Gson()
            val api =
                Retrofit
                    .Builder()
                    .baseUrl(server.url("/"))
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(WikidataInterface::class.java)
            val preferences: JsonKvStore = mock()
            whenever(preferences.getBoolean("Picture_Has_Correct_Location", true)).thenReturn(true)
            val edits = WikidataEditService(mock(), null, preferences, mock(), WikidataClient(api, gson), mock())
            val savedPlace = WikidataPlace("Q42", "Test place", null, null)

            edits.createClaim(savedPlace, "First.jpg", mapOf("en" to "First photo"))
            edits.createClaim(savedPlace, "Second.jpg", mapOf("en" to "Second photo"))

            assertEquals(1, wikidata.imageWrites)
        }
    }

    /** Mimics an item whose first image write is visible to the second lookup. */
    private class ImageClaimDispatcher : Dispatcher() {
        @Volatile
        var imageWrites = 0
            private set

        /** Serves a fresh item, CSRF token, or successful claim response. */
        override fun dispatch(request: RecordedRequest): MockResponse {
            val body =
                when (request.requestUrl?.queryParameter("action")) {
                    "wbgetentities" -> {
                        val claims = if (imageWrites == 0) "{}" else """{"P18":[{}]}"""
                        """{"entities":{"Q42":{"id":"Q42","lastrevid":${42 + imageWrites},"claims":$claims}}}"""
                    }
                    "query" -> """{"query":{"tokens":{"csrftoken":"test-token"}}}"""
                    "wbsetclaim" -> {
                        imageWrites++
                        """{"pageinfo":{"lastrevid":${42 + imageWrites}},"success":1}"""
                    }
                    else -> return MockResponse().setResponseCode(404)
                }
            return MockResponse().setBody(body)
        }
    }
}
