package fr.free.nrw.commons.mwapi;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.mwapi.request.HttpClientFactory;
import fr.free.nrw.commons.mwapi.request.RequestBuilder;
import fr.free.nrw.commons.mwapi.response.EditApiResponse;
import fr.free.nrw.commons.mwapi.response.LoginApiResponse;
import fr.free.nrw.commons.mwapi.response.ParseApiResponse;
import fr.free.nrw.commons.mwapi.response.QueryApiResponse;
import fr.free.nrw.commons.mwapi.response.QueryApiResponse.QueryResponse.NotificationQueryResponse.NotificationResponse;
import fr.free.nrw.commons.notification.Notification;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class JsonMediaWikiApi implements MediaWikiApi {
    private static final String THUMB_SIZE = "640";
    private static final String CATEGORIES_NAMESPACE = "14";

    private final CookieManager cookieHandler;
    private final OkHttpClient okHttpClient;
    private final String apiHost;
    private final SharedPreferences sharedPreferences;
    private final HttpUrl uploadsPerUser;

    public JsonMediaWikiApi(String apiHost, String wikimediaForge, SharedPreferences sharedPreferences) {
        this.apiHost = apiHost;
        this.sharedPreferences = sharedPreferences;
        this.cookieHandler = new CookieManager();
        this.okHttpClient = HttpClientFactory.create(cookieHandler);
        this.uploadsPerUser = HttpUrl.parse(wikimediaForge +
                "urbanecmbot/uploadsbyuser/uploadsbyuser.py");

        RequestBuilder.use(okHttpClient, new Gson(), apiHost);
    }

    @Override
    public String getAuthCookie() {
        List<HttpCookie> cookies = cookieHandler.getCookieStore().getCookies();
        StringBuilder sb = new StringBuilder();
        for (HttpCookie cookie : cookies) {
            sb.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
        }
        return sb.toString();
    }

    @Override
    public void setAuthCookie(String authCookie) {
        URI uri;
        try {
            uri = new URI(apiHost);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        String[] parts = authCookie.split(";");
        for (String cookieString : parts) {
            String[] cookieParts = cookieString.split("=");
            HttpCookie httpCookie = new HttpCookie(cookieParts[0], cookieParts[1]);
            httpCookie.setDomain(apiHost);
            cookieHandler.getCookieStore().add(uri, httpCookie);
        }
    }

    @Override
    public String login(String username, String password) {
        String loginToken = getLoginToken();
        String statusCodeToReturn = login()
                .param("loginreturnurl", "https://commons.wikimedia.org")
                .param("rememberMe", "1")
                .param("logintoken", loginToken)
                .param("username", username)
                .param("password", password)
                .execute()
                .clientlogin.getStatusCodeToReturn();
        setAuthCookieOnLogin("PASS".equals(statusCodeToReturn));
        return statusCodeToReturn; // TODO - process all the status values
    }

    @Override
    public String login(String username, String password, String twoFactorCode) {
        String loginToken = getLoginToken();
        String statusCodeToReturn = login()
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", loginToken)
                .param("logincontinue", "1")
                .param("OATHToken", twoFactorCode)
                .execute()
                .clientlogin.getStatusCodeToReturn();
        setAuthCookieOnLogin("PASS".equals(statusCodeToReturn));
        return statusCodeToReturn; // TODO - process all the status values
    }

    // This belongs elsewhere, probably the session manager?
    private void setAuthCookieOnLogin(boolean isLoggedIn) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isUserLoggedIn", isLoggedIn);
        if (isLoggedIn) {
            editor.putString("getAuthCookie", getAuthCookie());
        } else {
            editor.remove("getAuthCookie");
        }
        editor.apply();
    }

    private String getLoginToken() {
        return nonCachedQuery()
                .param("type", "login")
                .param("meta", "tokens")
                .execute()
                .query.tokens.loginToken;
    }

    @Override
    public boolean validateLogin() {
        return !query()
                .param("meta", "userinfo")  // get
                .execute()
                .query.userInfo.id.equals("0");
    }

    @Override
    public String getEditToken() {
        return query()
                .param("meta", "tokens")
                .param("type", "csrf")
                .execute()
                .query.tokens.csrfToken;
    }

    @Override
    public boolean fileExistsWithName(String fileName) {
        return query()
                .param("prop", "imageinfo")
                .param("titles", "File:" + fileName)
                .execute()
                .query.firstPage()
                .imageInfoCount() > 0;
    }

    @Override
    public boolean pageExists(String pageName) {
        return !query()
                .param("titles", pageName)
                .execute()
                .query.pages.containsKey("-1");
    }

    @Override
    public String findThumbnailByFilename(String filename) {
        return query()
                .param("prop", "imageinfo")
                .param("iiprop", "url")
                .param("iiurlwidth", THUMB_SIZE)
                .param("titles", filename)
                .execute()
                .query.firstPage().thumbUrl();
    }

    @Override
    public boolean logEvents(LogBuilder[] logBuilders) {
        boolean allSuccess = true;
        for (LogBuilder logBuilder : logBuilders) {
            try {
                Response response = okHttpClient.newCall(
                        new Request.Builder()
                                .get()
                                .url(logBuilder.toHttpUrl())
                                .build()
                ).execute();
                if (response.code() != 204) {
                    allSuccess = false;
                }
                Timber.d("EventLog hit %s", logBuilder.toUrlString());

            } catch (IOException e) {
                // Probably just ignore for now. Can be much more robust with a service, etc later on.
                Timber.d("IO Error, EventLog hit skipped");
            }
        }

        return allSuccess;
    }

    @Override
    public List<Media> getCategoryImages(String categoryName) { // TODO
        return null;
    }

    @NonNull
    @Override
    public UploadResult uploadFile(String filename, InputStream file, long dataLength, String pageContents, String editSummary, ProgressListener progressListener) {
        return new UploadResult("",""); // TODO ... and this is the big one!  :-)
    }

    @Nullable
    @Override
    public String edit(String editToken, String processedPageContent, String filename, String summary) {
        return editOperation("text", editToken, processedPageContent, filename, summary);
    }

    @Nullable
    @Override
    public String prependEdit(String editToken, String processedPageContent, String filename, String summary) {
        return editOperation("prependtext", editToken, processedPageContent, filename, summary);
    }

    @Nullable
    @Override
    public String appendEdit(String editToken, String processedPageContent, String filename, String summary) {
        return editOperation("appendtext", editToken, processedPageContent, filename, summary);
    }

    private String editOperation(String editType, String editToken, String processedPageContent, String filename, String summary) {
        return edit()
                .param("title", filename)
                .param("token", editToken)
                .param(editType, processedPageContent)
                .param("summary", summary)
                .execute()
                .edit.result;
    }

    @NonNull
    @Override
    public MediaResult fetchMediaByFilename(String filename) {
        String wikiContent = query()
                .param("prop", "revisions")
                .param("titles", filename)
                .param("rvprop", "content")
                .param("rvlimit", "1")
                .execute()
                .query.firstPage().wikiContent();

        String renderedXml = parse()
                .param("title", "File:" + filename)
                .param("text", wikiContent)
                .param("prop", "parsetree")
                .param("contentformat", "text/x-wiki")
                .param("contentmodel", "wikitext")
                .execute()
                .parsedContent();

        return new MediaResult(wikiContent, renderedXml);
    }

    @NonNull
    @Override
    public Observable<String> searchCategories(String filterValue, int searchCatsLimit) {
        return Observable.fromIterable(query()
                .param("list", "search")
                .param("srwhat", "text")
                .param("srnamespace", CATEGORIES_NAMESPACE)
                .param("srlimit", searchCatsLimit)
                .param("srsearch", filterValue)
                .execute()
                .query.categories());
    }

    @NonNull
    @Override
    public Observable<String> allCategories(String filter, int searchCatsLimit) {
        return Observable.fromIterable(query()
                .param("list", "allcategories")
                .param("acprefix", filter)
                .param("aclimit", searchCatsLimit)
                .execute()
                .query.allCategories());
    }

    @NonNull
    @Override
    public List<Notification> getNotifications() {
        return Observable.fromIterable(query()
                .param("meta", "notifications")
                .param("notprop", "list")
                .param("notformat", "model")
                .execute().query.getUsefulNotifications())
                .map(NotificationResponse::toNotification)
                .toList().blockingGet();
    }

    @NonNull
    @Override
    public Observable<String> searchTitles(String title, int searchCatsLimit) {
        return searchCategories(title, searchCatsLimit); // TODO - check usages and have them call searchCategories() directly
    }

    @Nullable
    @Override
    public String revisionsByFilename(String filename) {
        return query()
                .param("prop", "revisions")
                .param("rvprop", "timestamp|content")
                .param("titles", filename)
                .execute().query.firstPage().wikiContent();
    }

    @Override
    public boolean existingFile(String fileSha1) {
        return query()
                .param("list", "allimages")
                .param("aisha1", fileSha1)
                .execute()
                .query.imageCount() > 0;
    }

    @NonNull
    @Override
    public LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) {
        RequestBuilder.ParameterBuilder<QueryApiResponse> builder = query()
                .param("list", "logevents")
                .param("letype", "upload")
                .param("leprop", "title|timestamp|ids")
                .param("leuser", user)
                .param("lelimit", limit);

        if (!TextUtils.isEmpty(lastModified)) {
            builder.param("leend", lastModified);
        }
        if (!TextUtils.isEmpty(queryContinue)) {
            builder.param("lestart", queryContinue);
        }
        QueryApiResponse result = builder.execute();

        return new LogEventResult(result, "");
    }

    @NonNull
    @Override
    public Single<Integer> getUploadCount(String userName) {
        return Single.fromCallable(() -> {
            HttpUrl url = uploadsPerUser.newBuilder().addQueryParameter("user", userName).build();
            Call call = okHttpClient.newCall(new Request.Builder().url(url).get().build());
            Response response = call.execute();
            int count = 0;
            if (response.code() < 300) {
                ResponseBody body = response.body();
                if (body != null) {
                    count = Integer.parseInt(body.string().trim());
                }
            }
            return count;
        });
    }

    @NonNull
    private RequestBuilder.ParameterBuilder<QueryApiResponse> query() {
        // See: https://www.mediawiki.org/wiki/API:Main_page
        //
        //   "Per the HTTP specification, POST requests cannot be cached.
        //   Therefore, whenever you're reading data from the web service
        //   API, you should use GET requests, not POST."
        return RequestBuilder.get(QueryApiResponse.class).action("query");
    }

    @NonNull
    private RequestBuilder.ParameterBuilder<QueryApiResponse> nonCachedQuery() {
        // In some cases, cache avoidance is the desired behaviour though.
        return RequestBuilder.post(QueryApiResponse.class).action("query");
    }

    @NonNull
    private RequestBuilder.ParameterBuilder<EditApiResponse> edit() {
        return RequestBuilder.post(EditApiResponse.class).action("edit");
    }

    @NonNull
    private RequestBuilder.ParameterBuilder<ParseApiResponse> parse() {
        return RequestBuilder.post(ParseApiResponse.class).action("parse");
    }

    @NonNull
    private RequestBuilder.ParameterBuilder<LoginApiResponse> login() {
        return RequestBuilder.post(LoginApiResponse.class).action("clientlogin");
    }
}
