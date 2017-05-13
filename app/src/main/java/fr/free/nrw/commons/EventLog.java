package fr.free.nrw.commons;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import fr.free.nrw.commons.settings.Prefs;
import in.yuvi.http.fluent.Http;
import timber.log.Timber;

public class EventLog {

    private static CommonsApplication app;

    private static class LogTask extends AsyncTask<LogBuilder, Void, Boolean> {

        @Override
        protected Boolean doInBackground(LogBuilder... logBuilders) {

            boolean  allSuccess = true;
            // Not using the default URL connection, since that seems to have different behavior than the rest of the code
            for(LogBuilder logBuilder: logBuilders) {
                HttpURLConnection conn;
                try {
                    URL url = logBuilder.toUrl();
                    HttpResponse response = Http.get(url.toString()).use(CommonsApplication.createHttpClient()).asResponse();

                    if(response.getStatusLine().getStatusCode() != 204) {
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

    private static final String DEVICE;
    static {
        if (Build.MODEL.startsWith(Build.MANUFACTURER)) {
            DEVICE = Utils.capitalize(Build.MODEL);
        } else {
            DEVICE = Utils.capitalize(Build.MANUFACTURER) + " " + Build.MODEL;
        }
    }

    public static void setApp(CommonsApplication app) {
        EventLog.app = app;
    }

    public static class LogBuilder {
        private JSONObject data;
        private long rev;
        private String schema;

        private LogBuilder(String schema, long revision) {
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

        private URL toUrl() {
            JSONObject fullData = new JSONObject();
            try {
                fullData.put("schema", schema);
                fullData.put("revision", rev);
                fullData.put("wiki", CommonsApplication.EVENTLOG_WIKI);
                data.put("device", DEVICE);
                data.put("platform", "Android/" + Build.VERSION.RELEASE);
                data.put("appversion", "Android/" + BuildConfig.VERSION_NAME);
                fullData.put("event", data);
                return new URL(CommonsApplication.EVENTLOG_URL + "?" + Utils.urlEncode(fullData.toString()) + ";");
            } catch (MalformedURLException | JSONException e) {
                throw new RuntimeException(e);
            }
        }

        // force param disregards user preference
        // Use *only* for tracking the user preference change for EventLogging
        // Attempting to use anywhere else will cause kitten explosions
        public void log(boolean force) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
            if(!settings.getBoolean(Prefs.TRACKING_ENABLED, true) && !force) {
                return; // User has disabled tracking
            }
            LogTask logTask = new LogTask();
            logTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
        }

        public void log() {
            log(false);
        }

    }

    public static LogBuilder schema(String schema, long revision) {
        return new LogBuilder(schema, revision);
    }

    public static LogBuilder schema(Object[] scid) {
        if(scid.length != 2) {
            throw new IllegalArgumentException("Needs an object array with schema as first param and revision as second");
        }
        return schema((String)scid[0], (Long)scid[1]);
    }
}
