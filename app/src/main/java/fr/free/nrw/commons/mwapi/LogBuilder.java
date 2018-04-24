package fr.free.nrw.commons.mwapi;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.settings.Prefs;
import okhttp3.HttpUrl;

@SuppressWarnings("WeakerAccess")
public class LogBuilder {
    private final MediaWikiApi mwApi;
    private final Map<String, Object> data2;
    private final JSONObject data;
    private final long rev;
    private final String schema;
    private final SharedPreferences prefs;
    private final Gson gsonParser;

    /**
     * Main constructor of LogBuilder
     *
     * @param schema   Log schema
     * @param revision Log revision
     * @param mwApi    Wiki media API instance
     * @param prefs    Instance of SharedPreferences
     * @param gsonParser Json parser
     */
    LogBuilder(String schema, long revision, MediaWikiApi mwApi, SharedPreferences prefs, Gson gsonParser) {
        this.gsonParser = gsonParser;
        this.prefs = prefs;
        this.data = new JSONObject();
        this.data2 = new HashMap<>();
        this.schema = schema;
        this.rev = revision;
        this.mwApi = mwApi;
    }

    /**
     * Adds data to preferences
     * @param key Log key
     * @param value Log object value
     * @return LogBuilder
     */
    public LogBuilder param(String key, Object value) {
        try {
            data.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    HttpUrl toHttpUrl() {
        return HttpUrl.parse(toUrlString());
    }

    String toUrlString() {
        Map<String, Object> fullData = new HashMap<>();
        fullData.put("schema", schema);
        fullData.put("revision", rev);
        fullData.put("wiki", BuildConfig.EVENTLOG_WIKI);
        data2.put("device", EventLog.DEVICE);
        data2.put("platform", "Android/" + Build.VERSION.RELEASE);
        data2.put("appversion", "Android/" + BuildConfig.VERSION_NAME);
        fullData.put("event", data2);
        return BuildConfig.EVENTLOG_URL + "?" + Utils.urlEncode(gsonParser.toJson(fullData)) + ";";
    }

    /**
     * Encodes JSON object to URL
     * @return URL to JSON object
     */
    URL toUrl() {
        JSONObject fullData = new JSONObject();
        try {
            fullData.put("schema", schema);
            fullData.put("revision", rev);
            fullData.put("wiki", BuildConfig.EVENTLOG_WIKI);
            data.put("device", EventLog.DEVICE);
            data.put("platform", "Android/" + Build.VERSION.RELEASE);
            data.put("appversion", "Android/" + BuildConfig.VERSION_NAME);
            fullData.put("event", data);
            return new URL(BuildConfig.EVENTLOG_URL + "?" + Utils.urlEncode(fullData.toString()) + ";");
        } catch (MalformedURLException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // force param disregards user preference
    // Use *only* for tracking the user preference change for EventLogging
    // Attempting to use anywhere else will cause kitten explosions
    public void log(boolean force) {
        if (!prefs.getBoolean(Prefs.TRACKING_ENABLED, true) && !force) {
            return; // User has disabled tracking
        }
        LogTask logTask = new LogTask(mwApi);
        logTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }
    
    public void log() {
        log(false);
    }

}
