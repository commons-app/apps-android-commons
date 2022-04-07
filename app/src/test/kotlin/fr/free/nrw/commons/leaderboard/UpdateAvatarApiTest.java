package fr.free.nrw.commons.leaderboard;

import com.google.gson.Gson;
import fr.free.nrw.commons.profile.models.UpdateAvatarResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UpdateAvatarApiTest {

    private static final String TEST_USERNAME = "user";
    private static final String TEST_STATUS = "200";
    private static final String TEST_MESSAGE = "Avatar Updated";
    private static final String FILE_NAME = "update_leaderboard_avatar_sample_response.json";
    private static final String ENDPOINT = "/update_avatar.py";
    MockWebServer server;

    /**
     * This method converts a Input Stream to String
     *
     * @param is takes Input Stream of JSON File as Parameter
     * @return a String with JSON data
     * @throws Exception
     */
    private static String convertStreamToString(final InputStream is) throws Exception {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * This method initialises a Mock Server
     */
    @Before
    public void initTest() {
        server = new MockWebServer();
    }

    /**
     * This method will setup a Mock Server and load Test JSON Response File
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        final String testResponseBody = convertStreamToString(
            getClass().getClassLoader().getResourceAsStream(FILE_NAME));

        server.enqueue(new MockResponse().setBody(testResponseBody));
        server.start();
    }

    /**
     * This method will call the Mock Server and Test it with sample values. It will test the Update
     * Avatar API call functionality and check if the object is being created with the correct
     * values
     *
     * @throws IOException
     */
    @Test
    public void apiTest() throws IOException {
        final HttpUrl httpUrl = server.url(ENDPOINT);
        final UpdateAvatarResponse response = sendRequest(new OkHttpClient(), httpUrl);

        Assert.assertEquals(TEST_USERNAME, response.getUser());
        Assert.assertEquals(TEST_STATUS, response.getStatus());
        Assert.assertEquals(TEST_MESSAGE, response.getMessage());
    }

    /**
     * This method will call the Mock API and returns the Update Avatar Response Object
     *
     * @param okHttpClient
     * @param httpUrl
     * @return Update Avatar Response Object
     * @throws IOException
     */
    private UpdateAvatarResponse sendRequest(final OkHttpClient okHttpClient, final HttpUrl httpUrl)
        throws IOException {
        final Request request = new Builder().url(httpUrl).build();
        final Response response = okHttpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            final Gson gson = new Gson();
            return gson.fromJson(response.body().string(), UpdateAvatarResponse.class);
        }
        return null;
    }

    /**
     * This method shuts down the Mock Server
     *
     * @throws IOException
     */
    @After
    public void shutdown() throws IOException {
        server.shutdown();
    }
}

