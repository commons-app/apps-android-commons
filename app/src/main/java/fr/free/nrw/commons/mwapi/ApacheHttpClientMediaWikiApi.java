package fr.free.nrw.commons.mwapi;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Utils;
import in.yuvi.http.fluent.Http;
import timber.log.Timber;

/**
 * @author Addshore
 */
public class ApacheHttpClientMediaWikiApi implements MediaWikiApi {
    private static final String THUMB_SIZE = "640";
    private AbstractHttpClient httpClient;
    private MWApi api;

    public ApacheHttpClientMediaWikiApi(String apiURL) {
        BasicHttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        params.setParameter(CoreProtocolPNames.USER_AGENT, "Commons/" + BuildConfig.VERSION_NAME + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE);
        httpClient = new DefaultHttpClient(cm, params);
        api = new MWApi(apiURL, httpClient);
    }

    /**
     * @param username String
     * @param password String
     * @return String as returned by this.getErrorCodeToReturn()
     * @throws IOException On api request IO issue
     */
    public String login(String username, String password) throws IOException {
        return getErrorCodeToReturn(api.action("clientlogin")
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", this.getLoginToken())
                .param("loginreturnurl", "https://commons.wikimedia.org")
                .post());
    }

    /**
     * @param username      String
     * @param password      String
     * @param twoFactorCode String
     * @return String as returned by this.getErrorCodeToReturn()
     * @throws IOException On api request IO issue
     */
    public String login(String username, String password, String twoFactorCode) throws IOException {
        return getErrorCodeToReturn(api.action("clientlogin")
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", getLoginToken())
                .param("logincontinue", "1")
                .param("OATHToken", twoFactorCode)
                .post());
    }

    private String getLoginToken() throws IOException {
        return api.action("query")
                .param("action", "query")
                .param("meta", "tokens")
                .param("type", "login")
                .post()
                .getString("/api/query/tokens/@logintoken");
    }

    /**
     * @param loginApiResult ApiResult Any clientlogin api result
     * @return String On success: "PASS"
     * continue: "2FA" (More information required for 2FA)
     * failure: A failure message code (defined by mediawiki)
     * misc:    genericerror-UI, genericerror-REDIRECT, genericerror-RESTART
     */
    private String getErrorCodeToReturn(ApiResult loginApiResult) {
        String status = loginApiResult.getString("/api/clientlogin/@status");
        if (status.equals("PASS")) {
            api.isLoggedIn = true;
            return status;
        } else if (status.equals("FAIL")) {
            return loginApiResult.getString("/api/clientlogin/@messagecode");
        } else if (
                status.equals("UI")
                        && loginApiResult.getString("/api/clientlogin/requests/_v/@id").equals("TOTPAuthenticationRequest")
                        && loginApiResult.getString("/api/clientlogin/requests/_v/@provider").equals("Two-factor authentication (OATH).")
                ) {
            return "2FA";
        }

        // UI, REDIRECT, RESTART
        return "genericerror-" + status;
    }

    @Override
    public String getAuthCookie() {
        return api.getAuthCookie();
    }

    @Override
    public void setAuthCookie(String authCookie) {
        api.setAuthCookie(authCookie);
    }

    @Override
    public boolean validateLogin() throws IOException {
        return api.validateLogin();
    }

    @Override
    public String getEditToken() throws IOException {
        return api.getEditToken();
    }

    @Override
    public boolean fileExistsWithName(String fileName) throws IOException {
        return api.action("query")
                .param("prop", "imageinfo")
                .param("titles", "File:" + fileName)
                .get()
                .getNodes("/api/query/pages/page/imageinfo").size() > 0;
    }

    @Override
    @Nullable
    public String edit(String editToken, String processedPageContent, String filename, String summary) throws IOException {
        return api.action("edit")
                .param("title", filename)
                .param("token", editToken)
                .param("text", processedPageContent)
                .param("summary", summary)
                .post()
                .getString("/api/edit/@result");
    }

    @Override
    public String findThumbnailByFilename(String filename) throws IOException {
        return api.action("query")
                .param("format", "xml")
                .param("prop", "imageinfo")
                .param("iiprop", "url")
                .param("iiurlwidth", THUMB_SIZE)
                .param("titles", filename)
                .get()
                .getString("/api/query/pages/page/imageinfo/ii/@thumburl");
    }

    @Override
    @NonNull
    public MediaResult fetchMediaByFilename(String filename) throws IOException {
        ApiResult apiResult = api.action("query")
                .param("prop", "revisions")
                .param("titles", filename)
                .param("rvprop", "content")
                .param("rvlimit", 1)
                .param("rvgeneratexml", 1)
                .get();

        return new MediaResult(
                apiResult.getString("/api/query/pages/page/revisions/rev"),
                apiResult.getString("/api/query/pages/page/revisions/rev/@parsetree"));
    }

    @Override
    @NonNull
    public List<String> searchCategories(int searchCatsLimit, String filterValue) throws IOException {
        List<ApiResult> categoryNodes = api.action("query")
                .param("format", "xml")
                .param("list", "search")
                .param("srwhat", "text")
                .param("srnamespace", "14")
                .param("srlimit", searchCatsLimit)
                .param("srsearch", filterValue)
                .get()
                .getNodes("/api/query/search/p/@title");

        if (categoryNodes == null) {
            return Collections.emptyList();
        }

        List<String> categories = new ArrayList<>();
        for (ApiResult categoryNode : categoryNodes) {
            String cat = categoryNode.getDocument().getTextContent();
            String catString = cat.replace("Category:", "");
            categories.add(catString);
        }

        return categories;
    }

    @Override
    @NonNull
    public List<String> allCategories(int searchCatsLimit, String filterValue) throws IOException {
        ArrayList<ApiResult> categoryNodes = api.action("query")
                .param("list", "allcategories")
                .param("acprefix", filterValue)
                .param("aclimit", searchCatsLimit)
                .get()
                .getNodes("/api/query/allcategories/c");

        if (categoryNodes == null) {
            return Collections.emptyList();
        }

        List<String> categories = new ArrayList<>();
        for (ApiResult categoryNode : categoryNodes) {
            categories.add(categoryNode.getDocument().getTextContent());
        }

        return categories;
    }

    @Override
    @NonNull
    public List<String> searchTitles(int searchCatsLimit, String title) throws IOException {
        ArrayList<ApiResult> categoryNodes = api.action("query")
                .param("format", "xml")
                .param("list", "search")
                .param("srwhat", "text")
                .param("srnamespace", "14")
                .param("srlimit", searchCatsLimit)
                .param("srsearch", title)
                .get()
                .getNodes("/api/query/search/p/@title");

        if (categoryNodes == null) {
            return Collections.emptyList();
        }

        List<String> titleCategories = new ArrayList<>();
        for (ApiResult categoryNode : categoryNodes) {
            String cat = categoryNode.getDocument().getTextContent();
            String catString = cat.replace("Category:", "");
            titleCategories.add(catString);
        }

        return titleCategories;
    }

    @Override
    @NonNull
    public LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException {
        org.mediawiki.api.MWApi.RequestBuilder builder = api.action("query")
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
        ApiResult result = builder.get();

        return new LogEventResult(
                getLogEventsFromResult(result),
                result.getString("/api/query-continue/logevents/@lestart"));
    }

    @NonNull
    private ArrayList<LogEventResult.LogEvent> getLogEventsFromResult(ApiResult result) {
        ArrayList<ApiResult> uploads = result.getNodes("/api/query/logevents/item");
        Timber.d("%d results!", uploads.size());
        ArrayList<LogEventResult.LogEvent> logEvents = new ArrayList<>();
        for (ApiResult image : uploads) {
            logEvents.add(new LogEventResult.LogEvent(
                    image.getString("@pageid"),
                    image.getString("@title"),
                    Utils.parseMWDate(image.getString("@timestamp")))
            );
        }
        return logEvents;
    }

    @Override
    @Nullable
    public String revisionsByFilename(String filename) throws IOException {
        return api.action("query")
                .param("prop", "revisions")
                .param("rvprop", "timestamp|content")
                .param("titles", filename)
                .get()
                .getString("/api/query/pages/page/revisions/rev");
    }

    @Override
    public boolean existingFile(String fileSha1) throws IOException {
        return api.action("query")
                .param("format", "xml")
                .param("list", "allimages")
                .param("aisha1", fileSha1)
                .get()
                .getNodes("/api/query/allimages/img").size() > 0;
    }

    @Override
    public boolean logEvents(LogBuilder[] logBuilders) {
        boolean allSuccess = true;
        // Not using the default URL connection, since that seems to have different behavior than the rest of the code
        for (LogBuilder logBuilder : logBuilders) {
            try {
                URL url = logBuilder.toUrl();
                HttpResponse response = Http.get(url.toString()).use(httpClient).asResponse();

                if (response.getStatusLine().getStatusCode() != 204) {
                    allSuccess = false;
                }
                Timber.d("EventLog hit %s", url);

            } catch (IOException e) {
                // Probably just ignore for now. Can be much more robust with a service, etc later on.
                Timber.d("IO Error, EventLog hit skipped");
            }
        }

        return allSuccess;
    }

    @Override
    @NonNull
    public UploadResult uploadFile(String filename, InputStream file, long dataLength, String pageContents, String editSummary, final ProgressListener progressListener) throws IOException {
        ApiResult result = api.upload(filename, file, dataLength, pageContents, editSummary, new in.yuvi.http.fluent.ProgressListener() {
            @Override
            public void onProgress(long transferred, long total) {
                progressListener.onProgress(transferred, total);
            }
        });
        String resultStatus = result.getString("/api/upload/@result");
        if (!resultStatus.equals("Success")) {
            String errorCode = result.getString("/api/error/@code");
            return new UploadResult(resultStatus, errorCode);
        } else {
            Date dateUploaded = Utils.parseMWDate(result.getString("/api/upload/imageinfo/@timestamp"));
            String canonicalFilename = "File:" + result.getString("/api/upload/@filename").replace("_", " "); // Title vs Filename
            String imageUrl = result.getString("/api/upload/imageinfo/@url");
            return new UploadResult(dateUploaded, canonicalFilename, imageUrl);
        }
    }
}
