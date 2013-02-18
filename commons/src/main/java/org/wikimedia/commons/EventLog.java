package org.wikimedia.commons;

import android.os.AsyncTask;
import de.akquinet.android.androlog.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class EventLog {
    private static class LogTask extends AsyncTask<LogBuilder, Void, Boolean> {

        @Override
        protected Boolean doInBackground(LogBuilder... logBuilders) {

            boolean  allSuccess = true;

            // Going to simply use the default URLConnection.
            // This should be as lightweight as possible, and doesn't really do any fancy stuff
            for(LogBuilder logBuilder: logBuilders) {
                HttpURLConnection conn;
                try {
                    URL url = logBuilder.toUrl();
                    conn = (HttpURLConnection) url.openConnection();
                    int respCode = conn.getResponseCode();
                    if(respCode != 204) {
                        allSuccess = false;
                    }
                    Log.d("Commons", "EventLog hit " + url.toString());

                } catch (IOException e) {
                    // Probably just ignore for now. Can be much more robust with a service, etc later on.
                    // But in the interest of debugging
                    throw new RuntimeException(e);
                }

            }

            return allSuccess;
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
                fullData.put("wiki", "commonswiki");
                fullData.put("event", data);
                return new URL(CommonsApplication.EVENTLOG_URL + "?" + Utils.urlEncode(fullData.toString()) + ";");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public void log() {
            LogTask logTask = new LogTask();
            Utils.executeAsyncTask(logTask, this);
        }

    }

    public static LogBuilder schema(String schema, long revision) {
        return new LogBuilder(schema, revision);
    }

    public static LogBuilder schema(Object[] schema_rev) {
        if(schema_rev.length != 2) {
            throw new IllegalArgumentException("Needs an object array with schema as first param and revision as second");
        }
        return schema((String)schema_rev[0], (Long)schema_rev[1]);
    }
}
