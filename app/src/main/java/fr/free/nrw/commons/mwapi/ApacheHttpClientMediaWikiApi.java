package fr.free.nrw.commons.mwapi;

import android.os.Build;
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

import java.io.IOException;
import java.net.URL;

import fr.free.nrw.commons.BuildConfig;
import in.yuvi.http.fluent.Http;
import timber.log.Timber;

/**
 * @author Addshore
 */
public class ApacheHttpClientMediaWikiApi extends org.mediawiki.api.MWApi implements MediaWikiApi {
    private static final String THUMB_SIZE = "640";
    private static AbstractHttpClient httpClient;

    public ApacheHttpClientMediaWikiApi(String apiURL) {
        super(apiURL, getHttpClient());
    }

    private static AbstractHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = newHttpClient();
        }
        return httpClient;
    }

    private static AbstractHttpClient newHttpClient() {
        BasicHttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        params.setParameter(CoreProtocolPNames.USER_AGENT, "Commons/" + BuildConfig.VERSION_NAME + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE);
        return new DefaultHttpClient(cm, params);
    }


    /**
     * @param username String
     * @param password String
     * @return String as returned by this.getErrorCodeToReturn()
     * @throws IOException On api request IO issue
     */
    public String login(String username, String password) throws IOException {
        return getErrorCodeToReturn(action("clientlogin")
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
        return getErrorCodeToReturn(action("clientlogin")
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", getLoginToken())
                .param("logincontinue", "1")
                .param("OATHToken", twoFactorCode)
                .post());
    }

    private String getLoginToken() throws IOException {
        return this.action("query")
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
            this.isLoggedIn = true;
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

    // Moved / consolidated methods
    @Override
    public boolean fileExistsWithName(String fileName) throws IOException {
        return action("query")
                .param("prop", "imageinfo")
                .param("titles", "File:" + fileName)
                .get()
                .getNodes("/api/query/pages/page/imageinfo").size() > 0;
    }

    @Override
    public ApiResult edit(String editToken, String processedPageContent, String filename, String summary) throws IOException {
        return action("edit")
                .param("title", filename)
                .param("token", editToken)
                .param("text", processedPageContent)
                .param("summary", summary)
                .post();
    }

    @Override
    public String findThumbnailByFilename(String filename) throws IOException {
        return action("query")
                .param("format", "xml")
                .param("prop", "imageinfo")
                .param("iiprop", "url")
                .param("iiurlwidth", THUMB_SIZE)
                .param("titles", filename)
                .get()
                .getString("/api/query/pages/page/imageinfo/ii/@thumburl");
    }

    @Override
    public ApiResult fetchMediaByFilename(String filename) throws IOException {
        return action("query")
                .param("prop", "revisions")
                .param("titles", filename)
                .param("rvprop", "content")
                .param("rvlimit", 1)
                .param("rvgeneratexml", 1)
                .get();
    }

    @Override
    public ApiResult searchCategories(int searchCatsLimit, String filterValue) throws IOException {
        return action("query")
                .param("format", "xml")
                .param("list", "search")
                .param("srwhat", "text")
                .param("srnamespace", "14")
                .param("srlimit", searchCatsLimit)
                .param("srsearch", filterValue)
                .get();
    }

    @Override
    public ApiResult allCategories(int searchCatsLimit, String filterValue) throws IOException {
        return action("query")
                .param("list", "allcategories")
                .param("acprefix", filterValue)
                .param("aclimit", searchCatsLimit)
                .get();
    }

    @Override
    public ApiResult searchTitles(int searchCatsLimit, String title) throws IOException {
        return action("query")
                .param("format", "xml")
                .param("list", "search")
                .param("srwhat", "text")
                .param("srnamespace", "14")
                .param("srlimit", searchCatsLimit)
                .param("srsearch", title)
                .get();
    }

    @Override
    public ApiResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException {
        org.mediawiki.api.MWApi.RequestBuilder builder = action("query")
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
        return builder.get();
    }

    @Override
    public ApiResult revisionsByFilename(String filename) throws IOException {
        return action("query")
                .param("prop", "revisions")
                .param("rvprop", "timestamp|content")
                .param("titles", filename)
                .get();
    }

    @Override
    public ApiResult existingFile(String fileSha1) throws IOException {
        return action("query")
                .param("format", "xml")
                .param("list", "allimages")
                .param("aisha1", fileSha1)
                .get();
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
}
