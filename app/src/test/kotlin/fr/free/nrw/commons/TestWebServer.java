package fr.free.nrw.commons;

import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class TestWebServer {
    public static final int TIMEOUT_DURATION = 5;
    public static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final MockWebServer server;

    public TestWebServer() {
        server = new MockWebServer();
    }

    public void setUp() throws IOException {
        server.start();
    }

    public void tearDown() throws IOException {
        server.shutdown();
    }

    public String getUrl() {
        return getUrl("");
    }

    public String getUrl(String path) {
        return server.url(path).url().toString();
    }

    public int getRequestCount() {
        return server.getRequestCount();
    }

    public void enqueue(@NonNull String body) {
        enqueue(new MockResponse().setBody(body));
    }

    public void enqueue(MockResponse response) {
        server.enqueue(response);
    }

    @NonNull public RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest req = server.takeRequest(TIMEOUT_DURATION,
                TIMEOUT_UNIT);
        if (req == null) {
            throw new InterruptedException("Timeout elapsed.");
        }
        return req;
    }
}
