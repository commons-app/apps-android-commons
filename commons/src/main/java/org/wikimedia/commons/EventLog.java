package org.wikimedia.commons;

import android.os.*;
import android.util.*;
import in.yuvi.http.fluent.Http;
import org.apache.http.HttpResponse;
import org.json.*;
import java.io.IOException;
import java.net.*;

public class EventLog {

    // Set to false in CommonsApplication if the user has disabled tracking
    public static boolean enabled = true;

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
                    Log.d("Commons", "EventLog hit " + url.toString());

                } catch (IOException e) {
                    // Probably just ignore for now. Can be much more robust with a service, etc later on.
                    Log.d("Commons", "IO Error, EventLog hit skipped");
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
                data.put("appversion", "Android/" + CommonsApplication.APPLICATION_VERSION);
                fullData.put("event", data);
                return new URL(CommonsApplication.EVENTLOG_URL + "?" + Utils.urlEncode(fullData.toString()) + ";");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public void log() {
            if(!enabled) {
                return; // User has disabled tracking
            }
            LogTask logTask = new LogTask();
            Utils.executeAsyncTask(logTask, this);
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
