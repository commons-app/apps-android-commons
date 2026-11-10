package fr.free.nrw.commons.media

import fr.free.nrw.commons.wikidata.GsonUtil.defaultGson
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/** Exercises the metadata refresh against a real HTTP cache. */
class MediaInterfaceCacheTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    /** The refresh must fetch the updated description even if a cached response is still fresh. */
    @Test
    fun refreshByTitleDoesNotReuseFreshCacheEntry() {
        MockWebServer().use { server ->
            Cache(tempFolder.newFolder(), 1024L * 1024).use { cache ->
                val mediaInterface =
                    Retrofit
                        .Builder()
                        .baseUrl(server.url("/"))
                        .client(OkHttpClient.Builder().cache(cache).build())
                        .addConverterFactory(GsonConverterFactory.create(defaultGson))
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .build()
                        .create(MediaInterface::class.java)
                val filename = "File:Example.jpg"
                server.enqueue(mediaResponse("Old description"))
                server.enqueue(mediaResponse("Updated description"))
                server.enqueue(mediaResponse("Unused response"))

                assertEquals("Old description", mediaInterface.getMedia(filename).blockingGet().imageDescription())
                assertEquals("Old description", mediaInterface.getMedia(filename).blockingGet().imageDescription())
                assertEquals(1, server.requestCount)

                assertEquals(
                    "Updated description",
                    mediaInterface.getMediaSuppressingErrors(filename).blockingGet().imageDescription(),
                )
                assertEquals(2, server.requestCount)

                mediaInterface.getMediaById("42").blockingGet()
                server.takeRequest()
                server.takeRequest()
                val idRefresh = server.takeRequest()
                assertEquals("42", idRefresh.requestUrl?.queryParameter("pageids"))
                assertEquals("no-cache", idRefresh.getHeader("Cache-Control"))
            }
        }
    }

    /** Returns a cacheable file metadata response with the supplied description. */
    private fun mediaResponse(description: String): MockResponse =
        MockResponse()
            .setHeader("Cache-Control", "max-age=3600")
            .setBody(
                """{"query":{"pages":[{"pageid":42,"title":"File:Example.jpg","imageinfo":[""" +
                    """{"extmetadata":{"ImageDescription":{"value":"$description"}}}]}]}}""",
            )

    /** Reads the description field consumed by the media mapper. */
    private fun MwQueryResponse.imageDescription(): String? =
        query()
            ?.firstPage()
            ?.imageInfo()
            ?.getMetadata()
            ?.imageDescription()
}
