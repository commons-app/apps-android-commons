package fr.free.nrw.commons.mwapi;

import android.os.AsyncTask;

class LogTask extends AsyncTask<LogBuilder, Void, Boolean> {

    private final MediaWikiApi mwApi;

    public LogTask(MediaWikiApi mwApi) {
        this.mwApi = mwApi;
    }

    @Override
    protected Boolean doInBackground(LogBuilder... logBuilders) {
        return mwApi.logEvents(logBuilders);
    }
}
