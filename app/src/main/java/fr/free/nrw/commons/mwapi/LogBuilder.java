package fr.free.nrw.commons.mwapi;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.settings.Prefs;

public class LogBuilder {
    private JSONObject data;
    private long rev;
    private String schema;

    LogBuilder(String schema, long revision) {
        data = new JSONObject();
        this.schema = schema;
        this.rev = revision;
    }

    public LogBuilder param(String key, Object value) {
        try {
            data.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CommonsApplication.getInstance());
        if (!settings.getBoolean(Prefs.TRACKING_ENABLED, true) && !force) {
            return; // User has disabled tracking
        }
        LogTask logTask = new LogTask();
        logTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }

    public void log() {
        log(false);
    }

}
