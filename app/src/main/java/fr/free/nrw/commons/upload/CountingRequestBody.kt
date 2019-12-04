package fr.free.nrw.commons.upload

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import java.io.IOException

/**
 * Decorates an OkHttp request body to count the number of bytes written when writing it. Can
 * decorate any request body, but is most useful for tracking the upload progress of large multipart
 * requests.
 *
 * @author Ashish Kumar
 */
class CountingRequestBody(protected var delegate: RequestBody, protected var listener: Listener) : RequestBody() {
    protected var countingSink: CountingSink? = null
    override fun contentType(): MediaType? {
        return delegate.contentType()
    }

    override fun contentLength(): Long {
        try {
            return delegate.contentLength()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        countingSink = CountingSink(sink)
        val bufferedSink = countingSink!!.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    protected inner class CountingSink(delegate: Sink?) : ForwardingSink(delegate!!) {
        private var bytesWritten: Long = 0
        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onRequestProgress(bytesWritten, contentLength())
        }
    }

    interface Listener {
        /**
         * Will be triggered when write progresses
         * @param bytesWritten
         * @param contentLength
         */
        fun onRequestProgress(bytesWritten: Long, contentLength: Long)
    }

}