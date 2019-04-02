package fr.free.nrw.commons.mwapi;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wikipedia.util.DateUtil;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.category.CategoryImageUtils;
import fr.free.nrw.commons.category.QueryContinue;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import timber.log.Timber;

/**
 * @author Addshore
 */
public class ApacheHttpClientMediaWikiApi implements MediaWikiApi {
    private static final String THUMB_SIZE = "640";
    private AbstractHttpClient httpClient;
    private CustomMwApi api;
    private CustomMwApi wikidataApi;
    private Context context;
    private JsonKvStore defaultKvStore;
    private Gson gson;

    private final String ERROR_CODE_BAD_TOKEN = "badtoken";

    public ApacheHttpClientMediaWikiApi(Context context,
                                        String apiURL,
                                        String wikidatApiURL,
                                        JsonKvStore defaultKvStore,
                                        Gson gson) {
        this.context = context;
        BasicHttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        params.setParameter(CoreProtocolPNames.USER_AGENT, CommonsApplication.getInstance().getUserAgent());
        httpClient = new DefaultHttpClient(cm, params);
        if (BuildConfig.DEBUG) {
            httpClient.addRequestInterceptor(NetworkInterceptors.getHttpRequestInterceptor());
        }
        api = new CustomMwApi(apiURL, httpClient);
        wikidataApi = new CustomMwApi(wikidatApiURL, httpClient);
        this.defaultKvStore = defaultKvStore;
        this.gson = gson;
    }

    /**
     * @param username String
     * @param password String
     * @return String as returned by this.getErrorCodeToReturn()
     * @throws IOException On api request IO issue
     */
    public String login(String username, String password) throws IOException {
        String loginToken = getLoginToken();
        Timber.d("Login token is %s", loginToken);
        return getErrorCodeToReturn(api.action("clientlogin")
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", loginToken)
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
        String loginToken = getLoginToken();
        Timber.d("Login token is %s", loginToken);
        return getErrorCodeToReturn(api.action("clientlogin")
                .param("rememberMe", "true")
                .param("username", username)
                .param("password", password)
                .param("logintoken", loginToken)
                .param("logincontinue", "true")
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
     * @param loginCustomApiResult CustomApiResult Any clientlogin api result
     * @return String On success: "PASS"
     * continue: "2FA" (More information required for 2FA)
     * failure: A failure message code (defined by mediawiki)
     * misc:    genericerror-UI, genericerror-REDIRECT, genericerror-RESTART
     */
    private String getErrorCodeToReturn(CustomApiResult loginCustomApiResult) {
        String status = loginCustomApiResult.getString("/api/clientlogin/@status");
        if (status.equals("PASS")) {
            api.isLoggedIn = true;
            setAuthCookieOnLogin(true);
            return status;
        } else if (status.equals("FAIL")) {
            setAuthCookieOnLogin(false);
            return loginCustomApiResult.getString("/api/clientlogin/@messagecode");
        } else if (
                status.equals("UI")
                        && loginCustomApiResult.getString("/api/clientlogin/requests/_v/@id").equals("TOTPAuthenticationRequest")
                        && loginCustomApiResult.getString("/api/clientlogin/requests/_v/@provider").equals("Two-factor authentication (OATH).")
                ) {
            setAuthCookieOnLogin(false);
            return "2FA";
        }

        // UI, REDIRECT, RESTART
        return "genericerror-" + status;
    }

    private void setAuthCookieOnLogin(boolean isLoggedIn) {
        if (isLoggedIn) {
            defaultKvStore.putBoolean("isUserLoggedIn", true);
            defaultKvStore.putString("getAuthCookie", api.getAuthCookie());
        } else {
            defaultKvStore.putBoolean("isUserLoggedIn", false);
            defaultKvStore.remove("getAuthCookie");
        }
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
        boolean validateLoginResp = api.validateLogin();
        Timber.d("Validate login response is %s", validateLoginResp);
        return validateLoginResp;
    }

    @Override
    public String getEditToken() throws IOException {
        String editToken = api.action("query")
                .param("meta", "tokens")
                .post()
                .getString("/api/query/tokens/@csrftoken");
        Timber.d("MediaWiki edit token is %s", editToken);
        return editToken;
    }

    @Override
    public String getCentralAuthToken() throws IOException {
        CustomApiResult result = api.action("centralauthtoken").get();
        String centralAuthToken = result.getString("/api/centralauthtoken/@centralauthtoken");

        Timber.d("MediaWiki Central auth token is %s", centralAuthToken);

        if ((centralAuthToken == null || centralAuthToken.isEmpty())
                && "notLoggedIn".equals(result.getString("api/error/@code"))) {
            Timber.d("Central auth token isn't valid. Trying to fetch a fresh token");
            api.removeAllCookies();
            String loginResultCode = login(AccountUtil.getUserName(context), AccountUtil.getPassword(context));
            if (loginResultCode.equals("PASS")) {
                return getCentralAuthToken();
            } else if (loginResultCode.equals("2FA")) {
                Timber.e("Cannot refresh session for 2FA enabled user. Login required");
            } else {
                Timber.e("Error occurred in refreshing session. Error code is %s", loginResultCode);
            }
        } else {
            Timber.e("Error occurred while fetching auth token. Error code is %s and message is %s",
                    result.getString("api/error/@code"),
                    result.getString("api/error/@info"));
        }
        return centralAuthToken;
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
    public Single<Boolean> pageExists(String pageName) {
        return Single.fromCallable(() -> Double.parseDouble(api.action("query")
                .param("titles", pageName)
                .get()
                .getString("/api/query/pages/page/@_idx")) != -1);
    }

    @Override
    public boolean thank(String editToken, long revision) throws IOException {
        CustomApiResult res = api.action("thank")
                .param("rev", revision)
                .param("token", editToken)
                .param("source", CommonsApplication.getInstance().getUserAgent())
                .post();
        String r = res.getString("/api/result/@success");
        // Does this correctly check the success/failure?
        // The docs https://www.mediawiki.org/wiki/Extension:Thanks seems unclear about that.
        return r.equals("success");
    }

    @Override
    @Nullable
    public String edit(String editToken, String processedPageContent, String filename, String summary) throws IOException {
        return api.action("edit")
                .param("title", filename)
                .param("token", getEditToken())
                .param("text", processedPageContent)
                .param("summary", summary)
                .post()
                .getString("/api/edit/@result");
    }



    @Override
    @Nullable
    public String appendEdit(String editToken, String processedPageContent, String filename, String summary) throws IOException {
        return api.action("edit")
                .param("title", filename)
                .param("token", getEditToken())
                .param("appendtext", processedPageContent)
                .param("summary", summary)
                .post()
                .getString("/api/edit/@result");
    }

    @Override
    @Nullable
    public String prependEdit(String editToken, String processedPageContent, String filename, String summary) throws IOException {
        return api.action("edit")
                .param("title", filename)
                .param("token", getEditToken())
                .param("prependtext", processedPageContent)
                .param("summary", summary)
                .post()
                .getString("/api/edit/@result");
    }

    @Override
    public Single<String> findThumbnailByFilename(String filename) {
        return Single.fromCallable(() -> api.action("query")
                .param("format", "xml")
                .param("prop", "imageinfo")
                .param("iiprop", "url")
                .param("iiurlwidth", THUMB_SIZE)
                .param("titles", filename)
                .get()
                .getString("/api/query/pages/page/imageinfo/ii/@thumburl"));
    }

    @Override
    public Single<String> parseWikicode(String source) {
        return Single.fromCallable(() -> api.action("flow-parsoid-utils")
                .param("from", "wikitext")
                .param("to", "html")
                .param("content", source)
                .param("title", "Main_page")
                .get()
                .getString("/api/flow-parsoid-utils/@content"));
    }

    @Override
    @NonNull
    public Single<MediaResult> fetchMediaByFilename(String filename) {
        return Single.fromCallable(() -> {
            CustomApiResult apiResult = api.action("query")
                    .param("prop", "revisions")
                    .param("titles", filename)
                    .param("rvprop", "content")
                    .param("rvlimit", 1)
                    .param("rvgeneratexml", 1)
                    .get();

            return new MediaResult(
                    apiResult.getString("/api/query/pages/page/revisions/rev"),
                    apiResult.getString("/api/query/pages/page/revisions/rev/@parsetree"));
        });
    }

    @Override
    @NonNull
    public Observable<String> searchCategories(String filterValue, int searchCatsLimit) {
        List<String> categories = new ArrayList<>();
        return Single.fromCallable(() -> {
            List<CustomApiResult> categoryNodes = null;
            try {
                categoryNodes = api.action("query")
                        .param("format", "xml")
                        .param("list", "search")
                        .param("srwhat", "text")
                        .param("srnamespace", "14")
                        .param("srlimit", searchCatsLimit)
                        .param("srsearch", filterValue)
                        .get()
                        .getNodes("/api/query/search/p/@title");
            } catch (IOException e) {
                Timber.e(e, "Failed to obtain searchCategories");
            }

            if (categoryNodes == null) {
                return new ArrayList<String>();
            }

            for (CustomApiResult categoryNode : categoryNodes) {
                String cat = categoryNode.getDocument().getTextContent();
                String catString = cat.replace("Category:", "");
                if (!categories.contains(catString)) {
                    categories.add(catString);
                }
            }

            return categories;
        }).flatMapObservable(Observable::fromIterable);
    }

    @Override
    @NonNull
    public Observable<String> allCategories(String filterValue, int searchCatsLimit) {
        return Single.fromCallable(() -> {
            ArrayList<CustomApiResult> categoryNodes = null;
            try {
                categoryNodes = api.action("query")
                        .param("list", "allcategories")
                        .param("acprefix", filterValue)
                        .param("aclimit", searchCatsLimit)
                        .get()
                        .getNodes("/api/query/allcategories/c");
            } catch (IOException e) {
                Timber.e(e, "Failed to obtain allCategories");
            }

            if (categoryNodes == null) {
                return new ArrayList<String>();
            }

            List<String> categories = new ArrayList<>();
            for (CustomApiResult categoryNode : categoryNodes) {
                categories.add(categoryNode.getDocument().getTextContent());
            }

            return categories;
        }).flatMapObservable(Observable::fromIterable);
    }

    @Override
    public String getWikidataCsrfToken() throws IOException {
        String wikidataCsrfToken = wikidataApi.action("query")
                .param("action", "query")
                .param("centralauthtoken", getCentralAuthToken())
                .param("meta", "tokens")
                .post()
                .getString("/api/query/tokens/@csrftoken");
        Timber.d("Wikidata csrf token is %s", wikidataCsrfToken);
        return wikidataCsrfToken;
    }

    /**
     * Creates a new claim using the wikidata API
     * https://www.mediawiki.org/wiki/Wikibase/API
     * @param entityId the wikidata entity to be edited
     * @param property the property to be edited, for eg P18 for images
     * @param snaktype the type of value stored for that property
     * @param value the actual value to be stored for the property, for eg filename in case of P18
     * @return returns revisionId if the claim is successfully created else returns null
     * @throws IOException
     */
    @Nullable
    @Override
    public String wikidatCreateClaim(String entityId, String property, String snaktype, String value) throws IOException {
        Timber.d("Filename is %s", value);
        CustomApiResult result = wikidataApi.action("wbcreateclaim")
                .param("entity", entityId)
                .param("centralauthtoken", getCentralAuthToken())
                .param("token", getWikidataCsrfToken())
                .param("snaktype", snaktype)
                .param("property", property)
                .param("value", value)
                .post();

        if (result == null || result.getNode("api") == null) {
            return null;
        }

        Node node = result.getNode("api").getDocument();
        Element element = (Element) node;

        if (element != null && element.getAttribute("success").equals("1")) {
            return result.getString("api/pageinfo/@lastrevid");
        } else {
            Timber.e(result.getString("api/error/@code") + " " + result.getString("api/error/@info"));
        }
        return null;
    }

    /**
     * Adds the wikimedia-commons-app tag to the edits made on wikidata
     * @param revisionId
     * @return
     * @throws IOException
     */
    @Nullable
    @Override
    public boolean addWikidataEditTag(String revisionId) throws IOException {
        CustomApiResult result = wikidataApi.action("tag")
                .param("revid", revisionId)
                .param("centralauthtoken", getCentralAuthToken())
                .param("token", getWikidataCsrfToken())
                .param("add", "wikimedia-commons-app")
                .param("reason", "Add tag for edits made using Android Commons app")
                .post();

        if (result == null || result.getNode("api") == null) {
            return false;
        }

        if ("success".equals(result.getString("api/tag/result/@status"))) {
            return true;
        } else {
            Timber.e("Error occurred in creating claim. Error code is: %s and message is %s",
                    result.getString("api/error/@code"),
                    result.getString("api/error/@info"));
        }
        return false;
    }

    @Override
    @NonNull
    public Observable<String> searchTitles(String title, int searchCatsLimit) {
        return Single.fromCallable((Callable<List<String>>) () -> {
            ArrayList<CustomApiResult> categoryNodes;

            try {
                categoryNodes = api.action("query")
                        .param("format", "xml")
                        .param("list", "search")
                        .param("srwhat", "text")
                        .param("srnamespace", "14")
                        .param("srlimit", searchCatsLimit)
                        .param("srsearch", title)
                        .get()
                        .getNodes("/api/query/search/p/@title");
            } catch (IOException e) {
                Timber.e(e, "Failed to obtain searchTitles");
                return Collections.emptyList();
            }

            if (categoryNodes == null) {
                return Collections.emptyList();
            }

            List<String> titleCategories = new ArrayList<>();
            for (CustomApiResult categoryNode : categoryNodes) {
                String cat = categoryNode.getDocument().getTextContent();
                String catString = cat.replace("Category:", "");
                titleCategories.add(catString);
            }

            return titleCategories;
        }).flatMapObservable(Observable::fromIterable);
    }

    @Override
    @NonNull
    public LogEventResult logEvents(String user, String lastModified, String queryContinue, int limit) throws IOException {
        CustomMwApi.RequestBuilder builder = api.action("query")
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
        CustomApiResult result = builder.get();

        return new LogEventResult(
                getLogEventsFromResult(result),
                result.getString("/api/query-continue/logevents/@lestart"));
    }

    @NonNull
    private ArrayList<LogEventResult.LogEvent> getLogEventsFromResult(CustomApiResult result) {
        ArrayList<CustomApiResult> uploads = result.getNodes("/api/query/logevents/item");
        Timber.d("%d results!", uploads.size());
        ArrayList<LogEventResult.LogEvent> logEvents = new ArrayList<>();
        for (CustomApiResult image : uploads) {
            logEvents.add(new LogEventResult.LogEvent(
                    image.getString("@pageid"),
                    image.getString("@title"),
                    parseMWDate(image.getString("@timestamp")))
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
    @NonNull
    public List<Notification> getNotifications(boolean archived) {
        CustomApiResult notificationNode = null;
        String notfilter;
        try {
            if (archived) {
                notfilter = "read";
            }else {
                notfilter = "!read";
            }
            String language=Locale.getDefault().getLanguage();
            if(StringUtils.isBlank(language)){
                //if no language is set we use the default user language defined on wikipedia
                language="user";
            }
            notificationNode = api.action("query")
                    .param("notprop", "list")
                    .param("format", "xml")
                    .param("meta", "notifications")
                    .param("notformat", "model")
                    .param("notwikis", "wikidatawiki|commonswiki|enwiki")
                    .param("notfilter", notfilter)
                    .param("uselang", language)
                    .get()
                    .getNode("/api/query/notifications/list");
        } catch (IOException e) {
            Timber.e(e, "Failed to obtain searchCategories");
        }

        if (notificationNode == null
                || notificationNode.getDocument() == null
                || notificationNode.getDocument().getChildNodes() == null
                || notificationNode.getDocument().getChildNodes().getLength() == 0) {
            return new ArrayList<>();
        }
        NodeList childNodes = notificationNode.getDocument().getChildNodes();
        return NotificationUtils.getNotificationsFromList(context, childNodes);
    }

    @Override
    public boolean markNotificationAsRead(Notification notification) throws IOException {
        Timber.d("Trying to mark notification as read: %s", notification.toString());
        String result = api.action("echomarkread")
                .param("token", getEditToken())
                .param("list", notification.notificationId)
                .post()
                .getString("/api/query/echomarkread/@result");

        if (StringUtils.isBlank(result)) {
            return false;
        }

        return result.equals("success");
    }

    /**
     * The method takes categoryName as input and returns a List of Subcategories
     * It uses the generator query API to get the subcategories in a category, 500 at a time.
     * Uses the query continue values for fetching paginated responses
     * @param categoryName Category name as defined on commons
     * @return
     */
    @Override
    @NonNull
    public List<String> getSubCategoryList(String categoryName) {
        CustomApiResult apiResult = null;
        try {
            CustomMwApi.RequestBuilder requestBuilder = api.action("query")
                    .param("generator", "categorymembers")
                    .param("format", "xml")
                    .param("gcmtype","subcat")
                    .param("gcmtitle", categoryName)
                    .param("prop", "info")
                    .param("gcmlimit", "500")
                    .param("iiprop", "url|extmetadata");

            apiResult = requestBuilder.get();
        } catch (IOException e) {
            Timber.e(e, "Failed to obtain searchCategories");
        }

        if (apiResult == null) {
            return new ArrayList<>();
        }

        CustomApiResult categoryImagesNode = apiResult.getNode("/api/query/pages");
        if (categoryImagesNode == null
                || categoryImagesNode.getDocument() == null
                || categoryImagesNode.getDocument().getChildNodes() == null
                || categoryImagesNode.getDocument().getChildNodes().getLength() == 0) {
            return new ArrayList<>();
        }

        NodeList childNodes = categoryImagesNode.getDocument().getChildNodes();
        return CategoryImageUtils.getSubCategoryList(childNodes);
    }

    /**
     * The method takes categoryName as input and returns a List of parent categories
     * It uses the generator query API to get the parent categories of a category, 500 at a time.
     * @param categoryName Category name as defined on commons
     * @return
     */
    @Override
    @NonNull
    public List<String> getParentCategoryList(String categoryName) {
        CustomApiResult apiResult = null;
        try {
            CustomMwApi.RequestBuilder requestBuilder = api.action("query")
                    .param("generator", "categories")
                    .param("format", "xml")
                    .param("titles", categoryName)
                    .param("prop", "info")
                    .param("cllimit", "500")
                    .param("iiprop", "url|extmetadata");

            apiResult = requestBuilder.get();
        } catch (IOException e) {
            Timber.e(e, "Failed to obtain parent Categories");
        }

        if (apiResult == null) {
            return new ArrayList<>();
        }

        CustomApiResult categoryImagesNode = apiResult.getNode("/api/query/pages");
        if (categoryImagesNode == null
                || categoryImagesNode.getDocument() == null
                || categoryImagesNode.getDocument().getChildNodes() == null
                || categoryImagesNode.getDocument().getChildNodes().getLength() == 0) {
            return new ArrayList<>();
        }

        NodeList childNodes = categoryImagesNode.getDocument().getChildNodes();
        return CategoryImageUtils.getSubCategoryList(childNodes);
    }

    /**
     * This method takes search keyword as input and returns a list of categories objects filtered using search query
     * It uses the generator query API to get the categories searched using a query, 25 at a time.
     * @param query keyword to search categories on commons
     * @return
     */
    @Override
    @NonNull
    public List<String> searchCategory(String query, int offset) {
        List<CustomApiResult> categoryNodes = null;
        try {
            categoryNodes = api.action("query")
                    .param("format", "xml")
                    .param("list", "search")
                    .param("srwhat", "text")
                    .param("srnamespace", "14")
                    .param("srlimit", "25")
                    .param("sroffset",offset)
                    .param("srsearch", query)
                    .get()
                    .getNodes("/api/query/search/p/@title");
        } catch (IOException e) {
            Timber.e(e, "Failed to obtain searchCategories");
        }

        if (categoryNodes == null) {
            return new ArrayList<>();
        }

        List<String> categories = new ArrayList<>();
        for (CustomApiResult categoryNode : categoryNodes) {
            String catName = categoryNode.getDocument().getTextContent();
            categories.add(catName);
        }
        return categories;
    }


    /**
     * For APIs that return paginated responses, MediaWiki APIs uses the QueryContinue to facilitate fetching of subsequent pages
     * https://www.mediawiki.org/wiki/API:Raw_query_continue
     * After fetching images a page of image for a particular category, shared defaultKvStore are updated with the latest QueryContinue Values
     * @param keyword
     * @param queryContinue
     */
    private void setQueryContinueValues(String keyword, QueryContinue queryContinue) {
        defaultKvStore.putString(keyword, gson.toJson(queryContinue));
    }

    /**
     * Before making a paginated API call, this method is called to get the latest query continue values to be used
     * @param keyword
     * @return
     */
    @Nullable
    private QueryContinue getQueryContinueValues(String keyword) {
        String queryContinueString = defaultKvStore.getString(keyword, null);
        return gson.fromJson(queryContinueString, QueryContinue.class);
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
    @NonNull
    public Single<UploadStash> uploadFile(
            String filename,
            @NonNull InputStream file,
            long dataLength,
            Uri fileUri,
            Uri contentProviderUri,
            ProgressListener progressListener) {
        return Single.fromCallable(() -> {
            CustomApiResult result = api.uploadToStash(filename, file, dataLength, getEditToken(), progressListener::onProgress);

            Timber.wtf("Result: " + result.toString());

            String resultStatus = result.getString("/api/upload/@result");
            if (!resultStatus.equals("Success")) {
                String errorCode = result.getString("/api/error/@code");
                Timber.e(errorCode);
                
                if (errorCode.equals(ERROR_CODE_BAD_TOKEN)) {
                    ViewUtil.showLongToast(context, R.string.bad_token_error_proposed_solution);
                }
                return new UploadStash(errorCode, resultStatus, filename, "");
            } else {
                String filekey = result.getString("/api/upload/@filekey");
                return new UploadStash("", resultStatus, filename, filekey);
            }
        });
    }


    @Override
    @NonNull
    public Single<UploadResult> uploadFileFinalize(
            String filename,
            String filekey,
            String pageContents,
            String editSummary) throws IOException {
        return Single.fromCallable(() -> {
            CustomApiResult result = api.uploadFromStash(
                    filename, filekey, pageContents, editSummary,
                    getEditToken());

            Timber.d("Result: %s", result.toString());

            String resultStatus = result.getString("/api/upload/@result");
            if (!resultStatus.equals("Success")) {
                String errorCode = result.getString("/api/error/@code");
                Timber.e(errorCode);

                if (errorCode.equals(ERROR_CODE_BAD_TOKEN)) {
                    ViewUtil.showLongToast(context, R.string.bad_token_error_proposed_solution);
                }
                return new UploadResult(resultStatus, errorCode);
            } else {
                Date dateUploaded = parseMWDate(result.getString("/api/upload/imageinfo/@timestamp"));
                String canonicalFilename = "File:" + result.getString("/api/upload/@filename").replace("_", " "); // Title vs Filename
                String imageUrl = result.getString("/api/upload/imageinfo/@url");
                return new UploadResult(resultStatus, dateUploaded, canonicalFilename, imageUrl);
            }
        });
    }

    /**

     * Checks to see if a user is currently blocked from Commons
     * @return whether or not the user is blocked from Commons
     */
    @Override
    public boolean isUserBlockedFromCommons() {
        boolean userBlocked = false;
        try {
            CustomApiResult result = api.action("query")
                    .param("action", "query")
                    .param("format", "xml")
                    .param("meta", "userinfo")
                    .param("uiprop", "blockinfo")
                    .get();
            if (result != null) {
                String blockEnd = result.getString("/api/query/userinfo/@blockexpiry");
                if (blockEnd.equals("infinite")) {
                    userBlocked = true;
                } else if (!blockEnd.isEmpty()) {
                    Date endDate = parseMWDate(blockEnd);
                    Date current = new Date();
                    userBlocked = endDate.after(current);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return userBlocked;
    }

    private Date parseMWDate(String mwDate) {
        try {
            return DateUtil.getIso8601DateFormat().parse(mwDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls media wiki's logout API
     */
    public void logout() {
        try {
            api.logout();
        } catch (IOException e) {
            Timber.e(e, "Error occurred while logging out");
        }
    }

}
