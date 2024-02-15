package fr.free.nrw.commons;

import static fr.free.nrw.commons.TestConnectionFactoryKt.createTestClient;

import androidx.annotation.NonNull;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import fr.free.nrw.commons.wikidata.GsonUtil;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RunWith(RobolectricTestRunner.class)
public abstract class MockWebServerTest {
    private OkHttpClient okHttpClient;
    private final TestWebServer server = new TestWebServer();

    @Before public void setUp() throws Throwable {
        OkHttpConnectionFactory.CLIENT = createTestClient();
        okHttpClient = OkHttpConnectionFactory.CLIENT.newBuilder()
            .dispatcher(new Dispatcher(new ImmediateExecutorService())).build();
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

    public final class ImmediateExecutorService extends AbstractExecutorService {
        @Override public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @NonNull @Override public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override public boolean isShutdown() {
            throw new UnsupportedOperationException();
        }

        @Override public boolean isTerminated() {
            throw new UnsupportedOperationException();
        }

        @Override public boolean awaitTermination(long l, @NonNull TimeUnit timeUnit)
            throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    }

    public class ImmediateExecutor implements Executor {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    }
}
