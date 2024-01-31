package fr.free.nrw.commons

import okhttp3.OkHttpClient
import org.wikipedia.dataclient.okhttp.TestStubInterceptor
import org.wikipedia.dataclient.okhttp.UnsuccessfulResponseInterceptor

fun createTestClient(): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(UnsuccessfulResponseInterceptor())
    .addInterceptor(TestStubInterceptor())
    .build()
