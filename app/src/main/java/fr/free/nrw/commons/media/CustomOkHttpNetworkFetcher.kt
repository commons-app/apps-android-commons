package fr.free.nrw.commons.media

import android.os.Looper
import android.os.SystemClock
import com.facebook.imagepipeline.common.BytesRange
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.BaseNetworkFetcher
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.FetchState
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.producers.ProducerContext
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// Custom implementation of Fresco's Network fetcher to skip downloading of images when limited connection mode is enabled
// https://github.com/facebook/fresco/blob/master/imagepipeline-backends/imagepipeline-okhttp3/src/main/java/com/facebook/imagepipeline/backends/okhttp3/OkHttpNetworkFetcher.java
@Singleton
class CustomOkHttpNetworkFetcher
@JvmOverloads constructor(
    private val mCallFactory: Call.Factory,
    private val mCancellationExecutor: Executor,
    private val defaultKvStore: JsonKvStore,
    disableOkHttpCache: Boolean = true
) : BaseNetworkFetcher<OkHttpNetworkFetchState>() {

    private val mCacheControl =
        if (disableOkHttpCache) CacheControl.Builder().noStore().build() else null
    private val isLimitedConnectionMode: Boolean
        get() = defaultKvStore.getBoolean(
            CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
            false
        )

    /**
     * @param okHttpClient client to use
     */
    @Inject
    constructor(
        okHttpClient: OkHttpClient,
        @Named("default_preferences") defaultKvStore: JsonKvStore
    ) : this(okHttpClient, okHttpClient.dispatcher.executorService, defaultKvStore)

    /**
     * @param mCallFactory          custom [Call.Factory] for fetching image from the network
     * @param mCancellationExecutor executor on which fetching cancellation is performed if
     * cancellation is requested from the UI Thread
     * @param disableOkHttpCache   true if network requests should not be cached by OkHttp
     */
    override fun createFetchState(consumer: Consumer<EncodedImage>, context: ProducerContext) =
        OkHttpNetworkFetchState(consumer, context)

    override fun fetch(
        fetchState: OkHttpNetworkFetchState, callback: NetworkFetcher.Callback
    ) {
        fetchState.submitTime = SystemClock.elapsedRealtime()

        try {
            if (isLimitedConnectionMode) {
                Timber.d("Skipping loading of image as limited connection mode is enabled")
                callback.onFailure(Exception("Failing image request as limited connection mode is enabled"))
                return
            }

            val requestBuilder = Request.Builder().url(fetchState.uri.toString()).get()

            if (mCacheControl != null) {
                requestBuilder.cacheControl(mCacheControl)
            }

            val bytesRange = fetchState.context.imageRequest.bytesRange
            if (bytesRange != null) {
                requestBuilder.addHeader("Range", bytesRange.toHttpRangeHeaderValue())
            }

            fetchWithRequest(fetchState, callback, requestBuilder.build())
        } catch (e: Exception) {
            // handle error while creating the request
            callback.onFailure(e)
        }
    }

    override fun onFetchCompletion(fetchState: OkHttpNetworkFetchState, byteSize: Int) {
        fetchState.fetchCompleteTime = SystemClock.elapsedRealtime()
    }

    override fun getExtraMap(fetchState: OkHttpNetworkFetchState, byteSize: Int) =
        fetchState.toExtraMap(byteSize)

    private fun fetchWithRequest(
        fetchState: OkHttpNetworkFetchState, callback: NetworkFetcher.Callback, request: Request
    ) {
        val call = mCallFactory.newCall(request)

        fetchState.context.addCallbacks(object : BaseProducerContextCallbacks() {
            override fun onCancellationRequested() {
                onFetchCancellationRequested(call)
            }
        })

        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) =
                onFetchResponse(fetchState, call, response, callback)

            override fun onFailure(call: Call, e: IOException) =
                handleException(call, e, callback)
        })
    }

    private fun onFetchCancellationRequested(call: Call) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            call.cancel()
        } else {
            mCancellationExecutor.execute { call.cancel() }
        }
    }

    private fun onFetchResponse(
        fetchState: OkHttpNetworkFetchState,
        call: Call,
        response: Response,
        callback: NetworkFetcher.Callback
    ) {
        fetchState.responseTime = SystemClock.elapsedRealtime()
        try {
            response.body.use { body ->
                if (!response.isSuccessful) {
                    handleException(call, IOException("Unexpected HTTP code $response"), callback)
                    return
                }
                val responseRange =
                    BytesRange.fromContentRangeHeader(response.header("Content-Range"))
                if (responseRange != null && !(responseRange.from == 0 && responseRange.to == BytesRange.TO_END_OF_CONTENT)) {
                    // Only treat as a partial image if the range is not all of the content
                    fetchState.responseBytesRange = responseRange
                    fetchState.onNewResultStatusFlags = Consumer.IS_PARTIAL_RESULT
                }

                var contentLength = body!!.contentLength()
                if (contentLength < 0) {
                    contentLength = 0
                }
                callback.onResponse(body.byteStream(), contentLength.toInt())
            }
        } catch (e: Exception) {
            handleException(call, e, callback)
        }
    }

    /**
     * Handles exceptions.
     *
     * OkHttp notifies callers of cancellations via an IOException. If IOException is caught
     * after request cancellation, then the exception is interpreted as successful cancellation and
     * onCancellation is called. Otherwise onFailure is called.
     */
    private fun handleException(call: Call, e: Exception, callback: NetworkFetcher.Callback) {
        if (call.isCanceled()) {
            callback.onCancellation()
        } else {
            callback.onFailure(e)
        }
    }
}

class OkHttpNetworkFetchState(
    consumer: Consumer<EncodedImage>?, producerContext: ProducerContext?
) : FetchState(consumer, producerContext) {
    var submitTime: Long = 0
    var responseTime: Long = 0
    var fetchCompleteTime: Long = 0

    fun toExtraMap(byteSize: Int) = buildMap {
        put(QUEUE_TIME, (responseTime - submitTime).toString())
        put(FETCH_TIME, (fetchCompleteTime - responseTime).toString())
        put(TOTAL_TIME, (fetchCompleteTime - submitTime).toString())
        put(IMAGE_SIZE, byteSize.toString())
    }

    companion object {
        private const val QUEUE_TIME = "queue_time"
        private const val FETCH_TIME = "fetch_time"
        private const val TOTAL_TIME = "total_time"
        private const val IMAGE_SIZE = "image_size"
    }
}

