package fr.free.nrw.commons.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
import org.wikipedia.util.DateUtil;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import io.reactivex.Observable;
import io.reactivex.Single;
import timber.log.Timber;

/**
 * @author Addshore
 */
public class ApacheHttpClientMediaWikiApi implements MediaWikiApi {
    private AbstractHttpClient httpClient;
    private CustomMwApi api;

    public ApacheHttpClientMediaWikiApi(String apiURL) {
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
    public String getFileEntityId(String fileName) throws IOException {
        return "M" + api.action("query")
                .param("prop", "info")
                .param("titles", fileName)
                .get()
                .getString("/api/query/pages/page/@pageid");
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

    /*@NonNull
    @Override
    public Observable<String> searchDepictions (String query, int searchDepictsLimit) {
        return Single.fromCallable(() -> {
            ArrayList<CustomApiResult> depictionNodes = null;
            try {
                depictionNodes = wikidataApi.action("wbsearchentities")
                        .param("search", query)
                        .param("format", "json")
                        .param("language", "en")
                        .param("uselang","en")
                        .param("type","item")
                        .param("limit", searchDepictsLimit)
                        .get()
                        .getNodes("/api/query/allcategories/c");
            } catch (IOException e) {
                Timber.e(e, "Failed to obtain allCategories");
            }

            if (depictionNodes == null) {
                return new ArrayList<String>();
            }

            List<String> categories = new ArrayList<>();
            for (CustomApiResult categoryNode : depictionNodes) {
                categories.add(categoryNode.getDocument().getTextContent());
            }

            return categories;
        }).flatMapObservable(Observable::fromIterable);
    }
*/


    /**
     * Send captions(Q42) to wikibase, using the wikibase entity obtained after uploading imageto commons
     * https://commons.wikimedia.org/wiki/Wikibase/API
     * @param fileEntityId entity id for file, page id
     * @param caption(labels) item to be uploaded to wikibase
     */

    @Nullable
    @Override
    public String wikidataAddLabels(String fileEntityId, Map<String, String> caption) throws IOException {
        CustomApiResult result = api.action("wbsetlabel")
                .param("format","json")
                .param("id", fileEntityId)
                .param("language",caption.keySet().toString().substring(1,caption.keySet().toString().length()-1))
                .param("token", getEditToken())
                .param("value", caption.values().toString().substring(1,caption.values().toString().length()-1))
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
     * Edits claim using the commons API by adding P180 tag for an image
     * https://commons.wikimedia.org/wiki/Wikibase/API
     * @param entityId the commons api entity to be edited
     * @param fileEntityId entity id for file, page id
     * @return returns revisionId if the claim is successfully created else returns null
     * @throws IOException
     */
    @Nullable
    @Override
    public String wikidataEditEntity(String entityId, String fileEntityId) throws IOException {

        /*
         String data = "{\"claims\":[{\n" +
            "\"mainsnak\": {\n" +
                "\"snaktype\": \"value\",\n" +
                        "\"property\": \"P180\",\n" +
                        "\"datavalue\": {\n" +
                    "\"value\": {\n" +
                        "\"entity-type\": \"item\",\n" +
                                "\"numeric-id\": $ENTITY_ID$,\n" +
                                "\"id\": \"Q$ENTITY_ID$\"\n" +
                    "},\n" +
                    "\"type\": \"wikibase-entityid\"\n" +
                "}\n" +
            "},\n" +
            "\"type\": \"statement\",\n" +
                    "\"rank\": \"preferred\"\n" +
                 "}]}";
         data = data.replace("$ENTITY_ID$", entityId.replace("Q", ""));
        */

        JsonObject value = new JsonObject();
        value.addProperty("entity-type", "item");
        value.addProperty("numeric-id", "$ENTITY_ID$");
        value.addProperty("id", "Q$ENTITY_ID$");

        JsonObject dataValue = new JsonObject();
        dataValue.add("value", value);
        dataValue.addProperty("type", "wikibase-entityid");

        JsonObject mainSnak = new JsonObject();
        mainSnak.addProperty("snaktype", "value");
        mainSnak.addProperty("property", "P180");
        mainSnak.add("datavalue", dataValue);

        JsonObject claim = new JsonObject();
        claim.add("mainsnak", mainSnak);
        claim.addProperty("type", "statement");
        claim.addProperty("rank", "preferred");

        JsonArray claims = new JsonArray();
        claims.add(claim);

        JsonObject jsonData = new JsonObject();
        jsonData.add("claims", claims);

        String data = jsonData
                .toString()
                .replace("$ENTITY_ID$", entityId.replace("Q", ""));

        CustomApiResult result = api.action("wbeditentity")
                .param("id", fileEntityId)
                .param("token", getEditToken())
                .param("data", data)
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

    /**
     * Checks to see if a user is currently blocked from Commons
     *
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
            return DateUtil.iso8601DateParse(mwDate);
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
