package fr.free.nrw.commons;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import fr.free.nrw.commons.mwapi.MediaWikiApi;

class MediaThumbnailFetchTask extends AsyncTask<String, String, String> {
    protected final Media media;
    private MediaWikiApi mediaWikiApi;

    public MediaThumbnailFetchTask(@NonNull Media media, MediaWikiApi mwApi) {
        this.media = media;
        this.mediaWikiApi = mwApi;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            return mediaWikiApi.findThumbnailByFilename(params[0]);
        } catch (Exception e) {
            // Do something better!
        }
        return null;
    }
}
