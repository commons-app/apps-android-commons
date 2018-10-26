package fr.free.nrw.commons.mwapi;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.settings.Prefs;

@SuppressWarnings("WeakerAccess")
public class LogBuilder {
    private final MediaWikiApi mwApi;
    private final JSONObject data;
    private final long rev;
    private final String schema;
    private final SharedPreferences prefs;

    /**
     * Main constructor of LogBuilder
     *
     * @param schema   Log schema
     * @param revision Log revision
     * @param mwApi    Wiki media API instance
     * @param prefs    Instance of SharedPreferences
     */
    LogBuilder(String schema, long revision, MediaWikiApi mwApi, SharedPreferences prefs) {
        this.prefs = prefs;
        this.data = new JSONObject();
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
