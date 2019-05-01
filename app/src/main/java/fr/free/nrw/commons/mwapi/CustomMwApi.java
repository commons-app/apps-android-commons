package fr.free.nrw.commons.mwapi;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import in.yuvi.http.fluent.Http;
import in.yuvi.http.fluent.ProgressListener;
import timber.log.Timber;

public class CustomMwApi {
    public class RequestBuilder {
        private HashMap<String, Object> params;
        private CustomMwApi api;

        RequestBuilder(CustomMwApi api) {
            params = new HashMap<>();
            this.api = api;
        }

        public RequestBuilder param(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public CustomApiResult get() throws IOException {
            return api.makeRequest("GET", params);
        }

        public CustomApiResult post() throws IOException {
            return api.makeRequest("POST", params);
        }
    }

    private AbstractHttpClient client;
    private String apiURL;
    public boolean isLoggedIn;
    private String authCookie = null;
    private String userName = null;
    private String userID = null;

    public CustomMwApi(String apiURL, AbstractHttpClient client) {
        this.apiURL = apiURL;
        this.client = client;
    }

    public RequestBuilder action(String action) {
        RequestBuilder builder = new RequestBuilder(this);
        builder.param("action", action);
        return builder;
    }

    public String getAuthCookie() {
        if (authCookie == null){
            authCookie = "";
            List<Cookie> cookies = client.getCookieStore().getCookies();
            for(Cookie cookie: cookies) {
                authCookie += cookie.getName() + "=" + cookie.getValue() + ";";
            }
        }
        return authCookie;
    }

    public void setAuthCookie(String authCookie) {
        if (authCookie == null) {//If the authCookie is null, no need to proceed
            return;
        }

        this.authCookie = authCookie;
        this.isLoggedIn = true;
        String[] cookies = authCookie.split(";");
        String domain;
        try {
            domain = new URL(apiURL).getHost();
        } catch (MalformedURLException e) {
            // Mighty well better not happen!
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        // This works because I know which cookies are going to be set by MediaWiki, and they don't contain a = or ; in them :D
        for(String cookie: cookies) {
            String[] parts = cookie.split("=");
            BasicClientCookie c = new BasicClientCookie(parts[0], parts[1]);
            c.setDomain(domain);
            client.getCookieStore().addCookie(c);
        }
    }

    public void removeAllCookies() {
        client.getCookieStore().clear();
    }

    public boolean validateLogin() throws IOException {
        CustomApiResult userMeta = this.action("query").param("meta", "userinfo").get();
        this.userID = userMeta.getString("/api/query/userinfo/@id");
        this.userName = userMeta.getString("/api/query/userinfo/@name");
        Timber.d("User id is %s and user name is %s", userID, userName);
        return !userID.equals("0");
    }

    public String getUserID() throws IOException {
        if (this.userID == null || this.userID.equals("0")) {
            this.validateLogin();
        }
        return userID;
    }

    public String getUserName() throws IOException {
        if (this.userID == null || this.userID.equals("0")) {
            this.validateLogin();
        }
        return userName;
    }

    public String login(String username, String password) throws IOException {
        CustomApiResult tokenData = this.action("login").param("lgname", username).param("lgpassword", password).post();
        String result = tokenData.getString("/api/login/@result");
        if (result.equals("NeedToken")) {
            String token = tokenData.getString("/api/login/@token");
            CustomApiResult confirmData = this.action("login").param("lgname", username).param("lgpassword", password).param("lgtoken", token).post();
            String finalResult = confirmData.getString("/api/login/@result");
            if (finalResult.equals("Success")) {
                isLoggedIn = true;
            }
            return finalResult;
        } else {
            return result;
        }
    }

    public CustomApiResult uploadToStash(String filename, InputStream file, long length, String token, ProgressListener uploadProgressListener) throws IOException {
            Timber.d("Initiating upload for file %s", filename);
            Http.HttpRequestBuilder builder = Http.multipart(apiURL)
                .data("action", "upload")
                .data("stash", "1")
                .data("token", token)
                .data("ignorewarnings", "1")
                .data("filename", filename)
                .sendProgressListener(uploadProgressListener);
        if (length != -1) {
            builder.file("file", filename, file, length);
        } else {
            builder.file("file", filename, file);
        }

        return CustomApiResult.fromRequestBuilder("uploadToStash", builder, client);
    }

    public CustomApiResult uploadFromStash(String filename, String filekey, String text, String comment, String token) throws IOException {
        Http.HttpRequestBuilder builder = Http.multipart(apiURL)
                .data("action", "upload")
                .data("token", token)
                .data("ignorewarnings", "1")
                .data("text", text)
                .data("comment", comment)
                .data("filename", filename)
                .data("filekey", filekey);

        return CustomApiResult.fromRequestBuilder("uploadFromStash", builder, client);
    }

    public void logout() throws IOException {
        // I should be doing more validation here, but meh
        isLoggedIn = false;
        this.action("logout").post();
        removeAllCookies();
        authCookie = null;
    }

    private CustomApiResult makeRequest(String method, HashMap<String, Object> params) throws IOException {
        Http.HttpRequestBuilder builder;
        if (method.equals("POST")) {
            builder = Http.post(apiURL);
        } else {
            builder = Http.get(apiURL);
        }
        builder.data(params);
        return CustomApiResult.fromRequestBuilder(apiURL, builder, client);
    }
}
;
