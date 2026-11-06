package fr.free.nrw.commons

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

fun createTestClient(): OkHttpClient =
    OkHttpClient
        .Builder()
        .addInterceptor(UnsuccessfulResponseInterceptor())
        .addInterceptor(TestStubInterceptor())
        .build()

private class TestStubInterceptor : Interceptor {
    interface Callback {
        @Throws(IOException::class)
        fun getResponse(request: Interceptor.Chain): Response
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response =
        if (callback != null) {
            callback!!.getResponse(chain)
        } else {
            chain.proceed(chain.request())
        }

    companion object {
        var callback: Callback? = null
    }
}

private class UnsuccessfulResponseInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())
        if (rsp.isSuccessful) {
            return rsp
        }
        throw IOException("Unsuccessful response")
    }
}
