package fr.free.nrw.commons.media

import android.net.Uri
import com.facebook.imagepipeline.common.BytesRange
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.producers.ProducerContext
import com.facebook.imagepipeline.request.ImageRequest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import okhttp3.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import java.lang.reflect.Method
import java.util.concurrent.Executor

class CustomOkHttpNetworkFetcherUnitTest {

    private lateinit var fetcher: CustomOkHttpNetworkFetcher
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var state: CustomOkHttpNetworkFetcher.OkHttpNetworkFetchState

    @Mock
    private lateinit var callback: NetworkFetcher.Callback

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var consumer: Consumer<EncodedImage>

    @Mock
    private lateinit var context: ProducerContext

    @Mock
    private lateinit var imageRequest: ImageRequest

    @Mock
    private lateinit var uri: Uri

    @Mock
    private lateinit var mCacheControl: CacheControl

    @Mock
    private lateinit var bytesRange: BytesRange

    @Mock
    private lateinit var executor: Executor

    @Mock
    private lateinit var call: Call

    @Mock
    private lateinit var response: Response

    @Mock
    private lateinit var body: ResponseBody

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        okHttpClient = OkHttpClient()
        fetcher = CustomOkHttpNetworkFetcher(okHttpClient, defaultKvStore)
        whenever(context.imageRequest).thenReturn(imageRequest)
        whenever(imageRequest.sourceUri).thenReturn(uri)
        whenever(imageRequest.bytesRange).thenReturn(bytesRange)
        whenever(bytesRange.toHttpRangeHeaderValue()).thenReturn("bytes 200-1000/67589")
        whenever(uri.toString()).thenReturn("https://example.com")
        state = fetcher.createFetchState(consumer, context)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(fetcher)
    }

    @Test
    @Throws(Exception::class)
    fun testFetchCaseReturn() {
        whenever(
            defaultKvStore.getBoolean(
                CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                false
            )
        ).thenReturn(true)
        fetcher.fetch(state, callback)
        verify(callback).onFailure(any())
    }

    @Test
    @Throws(Exception::class)
    fun testFetch() {
        Whitebox.setInternalState(fetcher, "mCacheControl", mCacheControl)
        whenever(
            defaultKvStore.getBoolean(
                CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                false
            )
        ).thenReturn(false)
        fetcher.fetch(state, callback)
        fetcher.onFetchCompletion(state, 0)
        verify(bytesRange).toHttpRangeHeaderValue()
    }

    @Test
    @Throws(Exception::class)
    fun testFetchCaseException() {
        whenever(uri.toString()).thenReturn("")
        whenever(
            defaultKvStore.getBoolean(
                CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                false
            )
        ).thenReturn(false)
        fetcher.fetch(state, callback)
        verify(callback).onFailure(any())
    }

    @Test
    @Throws(Exception::class)
    fun testGetExtraMap() {
        val map = fetcher.getExtraMap(state, 40)
        Assertions.assertEquals(map!!["image_size"], 40.toString())
    }

    @Test
    @Throws(Exception::class)
    fun testOnFetchCancellationRequested() {
        Whitebox.setInternalState(fetcher, "mCancellationExecutor", executor)
        val method: Method = CustomOkHttpNetworkFetcher::class.java.getDeclaredMethod(
            "onFetchCancellationRequested", Call::class.java,
        )
        method.isAccessible = true
        method.invoke(fetcher, call)
        verify(executor).execute(any())
    }

    @Test
    @Throws(Exception::class)
    fun testOnFetchResponseCaseReturn() {
        whenever(response.body).thenReturn(body)
        whenever(response.isSuccessful).thenReturn(false)
        whenever(call.isCanceled()).thenReturn(true)
        val method: Method = CustomOkHttpNetworkFetcher::class.java.getDeclaredMethod(
            "onFetchResponse",
            CustomOkHttpNetworkFetcher.OkHttpNetworkFetchState::class.java,
            Call::class.java,
            Response::class.java,
            NetworkFetcher.Callback::class.java,
        )
        method.isAccessible = true
        method.invoke(fetcher, state, call, response, callback)
        verify(callback).onCancellation()
    }

    @Test
    @Throws(Exception::class)
    fun testOnFetchResponse() {
        whenever(response.body).thenReturn(body)
        whenever(response.isSuccessful).thenReturn(true)

        whenever(call.isCanceled()).thenReturn(true)
        whenever(body.contentLength()).thenReturn(-1)

        // Build Response object with Content-Range header
        val responseBuilder = Response.Builder()
            .request(Request.Builder().url("http://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Range", "bytes 200-1000/67589")
            .body(body)
        whenever(call.execute()).thenReturn(responseBuilder.build())

        val method: Method = CustomOkHttpNetworkFetcher::class.java.getDeclaredMethod(
            "onFetchResponse",
            CustomOkHttpNetworkFetcher.OkHttpNetworkFetchState::class.java,
            Call::class.java,
            Response::class.java,
            NetworkFetcher.Callback::class.java,
        )
        method.isAccessible = true
        method.invoke(fetcher, state, call, responseBuilder.build(), callback)
        verify(callback).onResponse(null, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnFetchResponseCaseException() {
        whenever(response.body).thenReturn(body)
        whenever(response.isSuccessful).thenReturn(true)

        whenever(call.isCanceled()).thenReturn(false)
        whenever(body.contentLength()).thenReturn(-1)

        // Build Response object with Content-Range header
        val responseBuilder = Response.Builder()
            .request(Request.Builder().url("http://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Range", "Test")
            .body(body)
        whenever(call.execute()).thenReturn(responseBuilder.build())

        val method: Method = CustomOkHttpNetworkFetcher::class.java.getDeclaredMethod(
            "onFetchResponse",
            CustomOkHttpNetworkFetcher.OkHttpNetworkFetchState::class.java,
            Call::class.java,
            Response::class.java,
            NetworkFetcher.Callback::class.java,
        )
        method.isAccessible = true
        method.invoke(fetcher, state, call, responseBuilder.build(), callback)
        verify(callback).onFailure(any())
    }

}