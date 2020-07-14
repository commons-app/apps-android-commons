package fr.free.nrw.commons.leaderboard;

import com.google.gson.Gson;
import fr.free.nrw.commons.profile.leaderboard.LeaderboardResponse;
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

public class LeaderboardApiTest {

    MockWebServer server;
    private static final String TEST_USERNAME = "user";
    private static final String TEST_AVATAR = "avatar";
    private static final int TEST_USER_RANK = 1;
    private static final int TEST_USER_COUNT = 0;

    private static final String FILE_NAME = "leaderboard_sample_response.json";
    private static final String ENDPOINT = "/leaderboard.py";

    /**
     * This method initialises a Mock Server
     */
    @Before
    public void initTest() {
        server = new MockWebServer();
    }

    /**
     * This method will setup a Mock Server and load Test JSON Response File
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        String testResponseBody = convertStreamToString(getClass().getClassLoader().getResourceAsStream(FILE_NAME));

        server.enqueue(new MockResponse().setBody(testResponseBody));
        server.start();
    }

    /**
     * This method converts a Input Stream to String
     * @param is takes Input Stream of JSON File as Parameter
     * @return a String with JSON data
     * @throws Exception
     */
    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * This method will call the Mock Server and Test it with sample values
     * @throws IOException
     */
    @Test
    public void apiTest() throws IOException {
        HttpUrl httpUrl = server.url(ENDPOINT);
        LeaderboardResponse response = sendRequest(new OkHttpClient(), httpUrl);

        Assert.assertEquals(TEST_AVATAR, response.getAvatar());
        Assert.assertEquals(TEST_USERNAME, response.getUsername());
        Assert.assertEquals(Integer.valueOf(TEST_USER_RANK), response.getRank());
        Assert.assertEquals(Integer.valueOf(TEST_USER_COUNT), response.getCategoryCount());
    }

    /**
     * This method will call the Mock API and returns the Leaderboard Response Object
     * @param okHttpClient
     * @param httpUrl
     * @return Leaderboard Response Object
     * @throws IOException
     */
    private LeaderboardResponse sendRequest(OkHttpClient okHttpClient, HttpUrl httpUrl)
        throws IOException {
        Request request = new Builder().url(httpUrl).build();
        Response response = okHttpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            Gson gson = new Gson();
            return gson.fromJson(response.body().string(), LeaderboardResponse.class);
        }
        return null;
    }

    /**
     * This method shuts down the Mock Server
     * @throws IOException
     */
    @After
    public void shutdown() throws IOException {
        server.shutdown();
    }
}
