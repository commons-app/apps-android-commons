package fr.free.nrw.commons;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import fr.free.nrw.commons.mwapi.MediaWikiApi;

class MediaThumbnailFetchTask extends AsyncTask<String, String, String> {
    protected final Media media;

    public MediaThumbnailFetchTask(@NonNull Media media) {
        this.media = media;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            MediaWikiApi api = CommonsApplication.getInstance().getMWApi();
            return api.findThumbnailByFilename(params[0]);
        } catch (Exception e) {
            // Do something better!
        }
        return null;
    }
}
