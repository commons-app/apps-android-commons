package fr.free.nrw.commons.mwapi;

import android.os.AsyncTask;

class LogTask extends AsyncTask<LogBuilder, Void, Boolean> {

    private final MediaWikiApi mwApi;

    /**
     * Main constructor of LogTask
     *
     * @param mwApi Media wiki API instance
     */
    public LogTask(MediaWikiApi mwApi) {
        this.mwApi = mwApi;
    }

    /**
     * Logs events in background
     * @param logBuilders LogBuilder instance
     * @return Background success state ( TRUE or FALSE )
     */
    @Override
    protected Boolean doInBackground(LogBuilder... logBuilders) {
        return mwApi.logEvents(logBuilders);
    }
}
