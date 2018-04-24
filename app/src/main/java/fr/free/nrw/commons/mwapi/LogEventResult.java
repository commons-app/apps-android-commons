package fr.free.nrw.commons.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.mwapi.response.ApiResponse;
import fr.free.nrw.commons.mwapi.response.QueryResponse;

public class LogEventResult {
    private final List<LogEvent> logEvents;
    private final String queryContinue;

    LogEventResult(@NonNull ApiResponse apiResponse, String queryContinue) {
        this.logEvents = getLogEventsFromResult(apiResponse);
        this.queryContinue = queryContinue;
    }

    LogEventResult(@NonNull List<LogEvent> logEvents, String queryContinue) {
        this.logEvents = logEvents;
        this.queryContinue = queryContinue;
    }

    @NonNull
    private ArrayList<LogEvent> getLogEventsFromResult(ApiResponse result) {
        List<QueryResponse.LogEventResponse> uploads = result.query != null
                ? result.query.logEvents : Collections.emptyList();
        ArrayList<LogEventResult.LogEvent> logEvents = new ArrayList<>();
        for (QueryResponse.LogEventResponse image : uploads) {
            logEvents.add(new LogEventResult.LogEvent(image.pageId, image.title,
                    Utils.parseMWDate(image.timestamp)));
        }
        return logEvents;
    }

    @NonNull
    public List<LogEvent> getLogEvents() {
        return logEvents;
    }

    @Nullable
    public String getQueryContinue() {
        return queryContinue;
    }

    public static class LogEvent {
        private final String pageId;
        private final String filename;
        private final Date dateUpdated;

        LogEvent(String pageId, String filename, Date dateUpdated) {
            this.pageId = pageId;
            this.filename = filename;
            this.dateUpdated = dateUpdated;
        }

        public boolean isDeleted() {
            return pageId.equals("0");
        }

        public String getFilename() {
            return filename;
        }

        public Date getDateUpdated() {
            return dateUpdated;
        }
    }
}
