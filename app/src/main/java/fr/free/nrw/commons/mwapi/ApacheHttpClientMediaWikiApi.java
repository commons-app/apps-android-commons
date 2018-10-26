package fr.free.nrw.commons.mwapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.gson.Gson;

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
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.category.CategoryImageUtils;
import fr.free.nrw.commons.category.QueryContinue;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationUtils;
import fr.free.nrw.commons.utils.ContributionUtils;
import fr.free.nrw.commons.utils.DateUtils;
import in.yuvi.http.fluent.Http;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static fr.free.nrw.commons.utils.ContinueUtils.getQueryContinue;

/**
 * @author Addshore
 */
public class ApacheHttpClientMediaWikiApi implements MediaWikiApi {
    private String wikiMediaToolforgeUrl = "https://tools.wmflabs.org/";

    private static final String THUMB_SIZE = "640";
    private AbstractHttpClient httpClient;
    private CustomMwApi api;
    private CustomMwApi wikidataApi;
    private Context context;
    private SharedPreferences defaultPreferences;
    private SharedPreferences categoryPreferences;
    private Gson gson;
    private final OkHttpClient okHttpClient;

    public ApacheHttpClientMediaWikiApi(Context context,
                                        String apiURL,
                                        String wikidatApiURL,
                                        SharedPreferences defaultPreferences,
                                        SharedPreferences categoryPreferences,
                                        Gson gson,
                                        OkHttpClient okHttpClient) {
        this.context = context;
        this.okHttpClient = okHttpClient;
        BasicHttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        params.setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
        httpClient = new DefaultHttpClient(cm, params);
        if (BuildConfig.DEBUG) {
            httpClient.addRequestInterceptor(NetworkInterceptors.getHttpRequestInterceptor());
        }
        api = new CustomMwApi(apiURL, httpClient);
        wikidataApi = new CustomMwApi(wikidatApiURL, httpClient);
        this.defaultPreferences = defaultPreferences;
        this.categoryPreferences = categoryPreferences;
        this.gson = gson;
    }

    @Override
    @NonNull
    public String getUserAgent() {
        return "Commons/" + BuildConfig.VERSION_NAME + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE;
    }

    @VisibleForTesting
    public void setWikiMediaToolforgeUrl(String wikiMediaToolforgeUrl) {
        this.wikiMediaToolforgeUrl = wikiMediaToolforgeUrl;
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
        SharedPreferences.Editor editor = defaultPreferences.edit();
        if (isLoggedIn) {
            editor.putBoolean("isUserLoggedIn", true);
            editor.putString("getAuthCookie", api.getAuthCookie());
        } else {
            editor.putBoolean("isUserLoggedIn", false);
            editor.remove("getAuthCookie");
        }
        editor.apply();
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
                .param("centralauthtoken", getCentralAuthToken())
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
    public boolean pageExists(String pageName) throws IOException {
        return Double.parseDouble( api.action("query")
                .param("titles", pageName)
                .get()
                .getString("/api/query/pages/page/@_idx")) != -1;
    }

    @Override
    @Nullable
    public String edit(String editToken, String processedPageContent, String filename, String summary) throws IOException {
        return api.action("edit")
                .param("title", filename)
                .param("token", getEditToken())
                .param("centralauthtoken", getCentralAuthToken())
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
                .param("centralauthtoken", getCentralAuthToken())
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
                .param("centralauthtoken", getCentralAuthToken())
                .param("prependtext", processedPageContent)
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
    }

    @Override
    @NonNull
    public Observable<String> searchCategories(String filterValue, int searchCatsLimit) {
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
                Timber.e("Failed to obtain searchCategories", e);
            }

            if (categoryNodes == null) {
                return new ArrayList<String>();
            }

            List<String> categories = new ArrayList<>();
            for (CustomApiResult categoryNode : categoryNodes) {
                String cat = categoryNode.getDocument().getTextContent();
                String catString = cat.replace("Category:", "");
                categories.add(catString);
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
                Timber.e("Failed to obtain allCategories", e);
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
                Timber.e("Failed to obtain searchTitles", e);
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
    public List<Notification> getNotifications() {
        CustomApiResult notificationNode = null;
        try {
            notificationNode = api.action("query")
                    .param("notprop", "list")
                    .param("format", "xml")
                    .param("meta", "notifications")
                    .param("notformat", "model")
                    .param("notwikis", "wikidatawiki|commonswiki|enwiki")
                    .get()
                    .getNode("/api/query/notifications/list");
        } catch (IOException e) {
            Timber.e("Failed to obtain searchCategories", e);
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
            Timber.e("Failed to obtain searchCategories", e);
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
            Timber.e("Failed to obtain parent Categories", e);
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
     * The method takes categoryName as input and returns a List of Media objects
     * It uses the generator query API to get the images in a category, 10 at a time.
     * Uses the query continue values for fetching paginated responses
     * @param categoryName Category name as defined on commons
     * @return
     */
    @Override
    @NonNull
    public List<Media> getCategoryImages(String categoryName) {
        CustomApiResult apiResult = null;
        try {
            CustomMwApi.RequestBuilder requestBuilder = api.action("query")
                    .param("generator", "categorymembers")
                    .param("format", "xml")
                    .param("gcmtype", "file")
                    .param("gcmtitle", categoryName)
                    .param("gcmsort", "timestamp")//property to sort by;timestamp
                    .param("gcmdir", "desc")//in which direction to sort;descending
                    .param("prop", "imageinfo")
                    .param("gcmlimit", "10")
                    .param("iiprop", "url|extmetadata");

            QueryContinue queryContinueValues = getQueryContinueValues(categoryName);
            if (queryContinueValues != null) {
                requestBuilder.param("continue", queryContinueValues.getContinueParam());
                requestBuilder.param("gcmcontinue", queryContinueValues.getGcmContinueParam());
            }
            apiResult = requestBuilder.get();
        } catch (IOException e) {
            Timber.e("Failed to obtain searchCategories", e);
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

        if (apiResult.getNode("/api/continue").getDocument()==null){
            setQueryContinueValues(categoryName, null);
        }else {
            QueryContinue queryContinue = getQueryContinue(apiResult.getNode("/api/continue").getDocument());
            setQueryContinueValues(categoryName, queryContinue);
        }

        NodeList childNodes = categoryImagesNode.getDocument().getChildNodes();
        return CategoryImageUtils.getMediaList(childNodes);
    }

    /**
     * This method takes search keyword as input and returns a list of  Media objects filtered using search query
     * It uses the generator query API to get the images searched using a query, 25 at a time.
     * @param query keyword to search images on commons
     * @return
     */
    @Override
    @NonNull
    public List<Media> searchImages(String query, int offset) {
        List<CustomApiResult> imageNodes = null;
        List<CustomApiResult> authorNodes = null;
        CustomApiResult customApiResult;
        try {
            customApiResult= api.action("query")
                    .param("format", "xml")
                    .param("generator", "search")
                    .param("gsrwhat", "text")
                    .param("gsrnamespace", "6")
                    .param("gsrlimit", "25")
                    .param("gsroffset",offset)
                    .param("gsrsearch", query)
                    .param("prop", "imageinfo")
                    .get();
            imageNodes= customApiResult.getNodes("/api/query/pages/page/@title");
            authorNodes= customApiResult.getNodes("/api/query/pages/page/imageinfo/ii/@user");
        } catch (IOException e) {
            Timber.e("Failed to obtain searchImages", e);
        }

        if (imageNodes == null) {
            return new ArrayList<Media>();
        }

        List<Media> images = new ArrayList<>();

        for (int i=0; i< imageNodes.size();i++){
            String imgName = imageNodes.get(i).getDocument().getTextContent();
            Media media = new Media(imgName);
            media.setCreator(authorNodes.get(i).getDocument().getTextContent());
            images.add(media);
        }
        return images;
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
            Timber.e("Failed to obtain searchCategories", e);
        }

        if (categoryNodes == null) {
            return new ArrayList<String>();
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
     * After fetching images a page of image for a particular category, shared prefs are updated with the latest QueryContinue Values
     * @param keyword
     * @param queryContinue
     */
    private void setQueryContinueValues(String keyword, QueryContinue queryContinue) {
        SharedPreferences.Editor editor = categoryPreferences.edit();
        editor.putString(keyword, gson.toJson(queryContinue));
        editor.apply();
    }

    /**
     * Before making a paginated API call, this method is called to get the latest query continue values to be used
     * @param keyword
     * @return
     */
    @Nullable
    private QueryContinue getQueryContinueValues(String keyword) {
        String queryContinueString = categoryPreferences.getString(keyword, null);
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
    public UploadResult uploadFile(String filename,
                                   @NonNull InputStream file,
                                   long dataLength,
                                   String pageContents,
                                   String editSummary,
                                   Uri fileUri,
                                   Uri contentProviderUri,
                                   final ProgressListener progressListener) throws IOException {

        CustomApiResult result = api.upload(filename, file, dataLength, pageContents, editSummary, getCentralAuthToken(), getEditToken(), progressListener::onProgress);

        Timber.wtf("Result: " + result.toString());

        String resultStatus = result.getString("/api/upload/@result");

        if (!resultStatus.equals("Success")) {
            String errorCode = result.getString("/api/error/@code");
            Timber.e(errorCode);
            return new UploadResult(resultStatus, errorCode);
        } else {
            // If success we have to remove file from temp directory
            ContributionUtils.removeTemporaryFile(fileUri);
            Date dateUploaded = parseMWDate(result.getString("/api/upload/imageinfo/@timestamp"));
            String canonicalFilename = "File:" + result.getString("/api/upload/@filename").replace("_", " "); // Title vs Filename
            String imageUrl = result.getString("/api/upload/imageinfo/@url");
            return new UploadResult(resultStatus, dateUploaded, canonicalFilename, imageUrl);
        }
    }

    @Override
    @NonNull
    public Single<Integer> getUploadCount(String userName) {
        final String uploadCountUrlTemplate =
                wikiMediaToolforgeUrl + "urbanecmbot/commonsmisc/uploadsbyuser.py";

        return Single.fromCallable(() -> {
            String url = String.format(
                    Locale.ENGLISH,
                    uploadCountUrlTemplate,
                    new PageTitle(userName).getText());
            HttpResponse response = Http.get(url).use(httpClient)
                    .data("user", userName)
                    .asResponse();
            String uploadCount = EntityUtils.toString(response.getEntity()).trim();
            return Integer.parseInt(uploadCount);
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

    /**
     * This takes userName as input, which is then used to fetch the feedback/achievements
     * statistics using OkHttp and JavaRx. This function return JSONObject
     * @param userName MediaWiki user name
     * @return
     */
    @Override
    public Single<FeedbackResponse> getAchievements(String userName) {
        final String fetchAchievementUrlTemplate =
                wikiMediaToolforgeUrl + "urbanecmbot/commonsmisc/feedback.py";
        return Single.fromCallable(() -> {
            String url = String.format(
                    Locale.ENGLISH,
                    fetchAchievementUrlTemplate,
                    new PageTitle(userName).getText());
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("user", userName);
            Timber.i("Url %s", urlBuilder.toString());
            Request request = new Request.Builder()
                    .url(urlBuilder.toString())
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                if (json == null) {
                    return null;
                }
                return gson.fromJson(json, FeedbackResponse.class);
            }
            return null;
        });

    }

    /**
     * The method returns the picture of the day
     *
     * @return Media object corresponding to the picture of the day
     */
    @Override
    @Nullable
    public Single<Media> getPictureOfTheDay() {
        return Single.fromCallable(() -> {
            CustomApiResult apiResult = null;
            try {
                String template = "Template:Potd/" + DateUtils.getCurrentDate();
                CustomMwApi.RequestBuilder requestBuilder = api.action("query")
                        .param("generator", "images")
                        .param("format", "xml")
                        .param("titles", template)
                        .param("prop", "imageinfo")
                        .param("iiprop", "url|extmetadata");

                apiResult = requestBuilder.get();
            } catch (IOException e) {
                Timber.e("Failed to obtain searchCategories", e);
            }

            if (apiResult == null) {
                return null;
            }

            CustomApiResult imageNode = apiResult.getNode("/api/query/pages/page");
            if (imageNode == null
                    || imageNode.getDocument() == null) {
                return null;
            }

            return CategoryImageUtils.getMediaFromPage(imageNode.getDocument());
        });
    }

    private Date parseMWDate(String mwDate) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH); // Assuming MW always gives me UTC
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return isoFormat.parse(mwDate);
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
