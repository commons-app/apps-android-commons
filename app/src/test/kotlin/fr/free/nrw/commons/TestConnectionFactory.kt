package fr.free.nrw.commons

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import fr.free.nrw.commons.OkHttpConnectionFactory.HttpStatusException
import java.io.IOException

fun createTestClient(): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(UnsuccessfulResponseInterceptor())
    .addInterceptor(TestStubInterceptor())
    .build()

private class TestStubInterceptor : Interceptor {
    interface Callback {
        @Throws(IOException::class)
        fun getResponse(request: Interceptor.Chain): Response
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return if (CALLBACK != null) {
            CALLBACK!!.getResponse(chain)
        } else chain.proceed(chain.request())
    }

    companion object {
        var CALLBACK: Callback? = null
    }
}

private class UnsuccessfulResponseInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())
        if (rsp.isSuccessful) {
            return rsp
        }
        throw HttpStatusException(rsp)
    }
}
