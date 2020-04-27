package org.wikipedia.test;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.AppAdapter;
import org.wikipedia.TestAppAdapter;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonUtil;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RunWith(RobolectricTestRunner.class)
public abstract class MockWebServerTest {
    private OkHttpClient okHttpClient;
    private final TestWebServer server = new TestWebServer();

    @Before public void setUp() throws Throwable {
        AppAdapter.set(new TestAppAdapter());
        OkHttpClient.Builder builder = AppAdapter.get().getOkHttpClient(new WikiSite(Service.WIKIPEDIA_URL)).newBuilder();
        okHttpClient = builder.dispatcher(new Dispatcher(new ImmediateExecutorService())).build();
        server.setUp();
    }

    @After public void tearDown() throws Throwable {
        server.tearDown();
    }

    @NonNull protected TestWebServer server() {
        return server;
    }

    protected void enqueueFromFile(@NonNull String filename) throws Throwable {
        String json = TestFileUtil.readRawFile(filename);
        server.enqueue(json);
    }

    protected void enqueue404() {
        final int code = 404;
        server.enqueue(new MockResponse().setResponseCode(code).setBody("Not Found"));
    }

    protected void enqueueMalformed() {
        server.enqueue("(╯°□°）╯︵ ┻━┻");
    }

    protected void enqueueEmptyJson() {
        server.enqueue(new MockResponse().setBody("{}"));
    }

    @NonNull protected OkHttpClient okHttpClient() {
        return okHttpClient;
    }

    @NonNull protected <T> T service(Class<T> clazz) {
        return service(clazz, server().getUrl());
    }

    @NonNull protected <T> T service(Class<T> clazz, @NonNull String url) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .callbackExecutor(new ImmediateExecutor())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build()
                .create(clazz);
    }
}
