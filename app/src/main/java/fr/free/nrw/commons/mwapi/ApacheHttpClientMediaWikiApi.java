package fr.free.nrw.commons.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;

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
import org.wikipedia.util.DateUtil;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;

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
