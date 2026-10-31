package fr.free.nrw.commons.upload

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import timber.log.Timber
import java.io.IOException

/**
 * Decorates an OkHttp request body to count the number of bytes written when writing it. Can
 * decorate any request body, but is most useful for tracking the upload progress of large multipart
 * requests.
 *
 * @author Ashish Kumar
 */
class CountingRequestBody(
    protected var delegate: RequestBody,
    protected var listener: Listener,
    var offset: Long,
    var totalContentLength: Long,
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long {
        try {
            return delegate.contentLength()
        } catch (e: IOException) {
            Timber.e(e, "Could not read content length")
        }
        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val bufferedSink = CountingSink(sink).buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(
        delegate: Sink?,
    ) : ForwardingSink(delegate!!) {
        private var bytesWritten: Long = 0

        @Throws(IOException::class)
        override fun write(
            source: Buffer,
            byteCount: Long,
        ) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onRequestProgress(offset + bytesWritten, totalContentLength)
        }
    }

    fun interface Listener {
        /**
         * Will be triggered when write progresses
         * @param bytesWritten
         * @param contentLength
         */
        fun onRequestProgress(
            bytesWritten: Long,
            contentLength: Long,
        )
    }
}
