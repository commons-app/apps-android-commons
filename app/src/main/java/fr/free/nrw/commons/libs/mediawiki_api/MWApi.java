/* Copyright (C) 2012 Yuvi Panda <yuvipanda@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.free.nrw.commons.libs.mediawiki_api;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;

import fr.free.nrw.commons.libs.http_fluent.Http;
import fr.free.nrw.commons.libs.http_fluent.ProgressListener;

public class MWApi {
    public class RequestBuilder {
        private HashMap<String, Object> params;
        private MWApi api;
        public Http.HttpRequestBuilder httpRequestBuilder;

        RequestBuilder(MWApi api) {
            params = new HashMap<String, Object>();
            this.api = api;
        }

        public RequestBuilder param(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public RequestBuilder prepareHttpRequestBuilder(String method){
            Http.HttpRequestBuilder builder;

            if (method == "POST") {
                builder = Http.post(apiURL);
            } else {
                builder = Http.get(apiURL);
            }
            builder.data(params);
            this.httpRequestBuilder = builder;
            return this;
        }

        public ApiResult request() throws IOException {
            return api.makeRequest(params, this);
        }

    }

    private AbstractHttpClient client;
    private String apiURL;
    public boolean isLoggedIn;
    private String authCookie = null;
    private String userName = null;
    private String userID = null;

    public MWApi(String apiURL, AbstractHttpClient client) {
        this.apiURL = apiURL;
        this.client = client;
    }

    public RequestBuilder action(String action) {
        RequestBuilder builder = new RequestBuilder(this);
        builder.param("action", action);
        return builder;
    }
    
    public String getAuthCookie() {
        if(authCookie == null){
            authCookie = "";
            List<Cookie> cookies = client.getCookieStore().getCookies();
            for(Cookie cookie: cookies) {
                authCookie += cookie.getName() + "=" + cookie.getValue() + ";";
            }
        }
        return authCookie;
    }
    
    public void setAuthCookie(String authCookie) {
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

    public boolean validateLogin() throws IOException {
        ApiResult userMeta = this.action("query").param("meta", "userinfo").prepareHttpRequestBuilder("GET").request();
        this.userID = userMeta.getString("/api/query/userinfo/@id");
        this.userName = userMeta.getString("/api/query/userinfo/@name");
        return !userID.equals("0");
    }
    
    public String getUserID() throws IOException {
        if(this.userID == null || this.userID == "0") {
            this.validateLogin();
        }
        return userID;
    }
    
    public String getUserName() throws IOException {
        if(this.userID == null || this.userID == "0") {
            this.validateLogin();
        }
        return userName;
    }
    
    public String login(String username, String password) throws IOException {
        ApiResult tokenData = this.action("login").param("lgname", username).param("lgpassword", password).prepareHttpRequestBuilder("POST").request();
        String result = tokenData.getString("/api/login/@result");
        if (result.equals("NeedToken")) {
            String token = tokenData.getString("/api/login/@token");
            ApiResult confirmData = this.action("login").param("lgname", username).param("lgpassword", password).param("lgtoken", token).prepareHttpRequestBuilder("POST").request();
            String finalResult = confirmData.getString("/api/login/@result");
            if(finalResult.equals("Success")) {
                isLoggedIn = true;
            }
            return finalResult;
        } else {
            return result;
        }
    }

    public ApiResult upload(String filename, InputStream file, long length, String text, String comment) throws IOException {
        return this.upload(filename, file, length, text, comment, null);
    }
    
    public ApiResult upload(String filename, InputStream file, String text, String comment) throws IOException {
        return this.upload(filename, file, -1, text, comment, null);
    }
    
    public ApiResult upload(String filename, InputStream file, long length, String text, String comment, ProgressListener uploadProgressListener) throws IOException {
        String token = this.getEditToken();
        Http.HttpRequestBuilder builder = Http.multipart(apiURL)
                .data("action", "upload")
                .data("token", token)
                .data("text", text)
                .data("ignorewarnings", "1")
                .data("comment", comment)
                .data("filename", filename)
                .sendProgressListener(uploadProgressListener);
        if(length != -1) {
                builder.file("file", filename, file, length);
        } else {
                builder.file("file", filename, file);
        }
        return ApiResult.fromRequestBuilder(builder, client);
    }
    
    public void logout() throws IOException {
        // I should be doing more validation here, but meh
        isLoggedIn = false;
        this.action("logout").prepareHttpRequestBuilder("POST").request();
    }

    public String getEditToken() throws IOException {
        ApiResult result = this.action("tokens").param("type", "edit").prepareHttpRequestBuilder("GET").request();
        return result.getString("/api/tokens/@edittoken");
    }

    private ApiResult makeRequest(HashMap<String, Object> params, RequestBuilder requestBuilder) throws IOException {
        return ApiResult.fromRequestBuilder(requestBuilder.httpRequestBuilder, client);
    }
}