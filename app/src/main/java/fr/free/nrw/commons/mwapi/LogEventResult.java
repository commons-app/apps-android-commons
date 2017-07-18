package fr.free.nrw.commons.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class LogEventResult {
    private final List<LogEvent> logEvents;
    private final String queryContinue;

    LogEventResult(@NonNull List<LogEvent> logEvents, String queryContinue) {
        this.logEvents = logEvents;
        this.queryContinue = queryContinue;
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
