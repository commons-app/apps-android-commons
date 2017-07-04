package fr.free.nrw.commons.mwapi;

import android.os.AsyncTask;

import fr.free.nrw.commons.CommonsApplication;

class LogTask extends AsyncTask<LogBuilder, Void, Boolean> {
    @Override
    protected Boolean doInBackground(LogBuilder... logBuilders) {
        return CommonsApplication.getInstance().getMWApi().logEvents(logBuilders);
    }
}
