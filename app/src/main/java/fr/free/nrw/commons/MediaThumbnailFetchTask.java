package fr.free.nrw.commons;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.mediawiki.api.ApiResult;

class MediaThumbnailFetchTask extends AsyncTask<String, String, String> {
    private static final String THUMB_SIZE = "640";
    protected final Media media;

    public MediaThumbnailFetchTask(@NonNull Media media) {
        this.media = media;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            MWApi api = CommonsApplication.getInstance().getMWApi();
            ApiResult result =api.action("query")
                    .param("format", "xml")
                    .param("prop", "imageinfo")
                    .param("iiprop", "url")
                    .param("iiurlwidth", THUMB_SIZE)
                    .param("titles", params[0])
                    .get();
            return result.getString("/api/query/pages/page/imageinfo/ii/@thumburl");
        } catch (Exception e) {
            // Do something better!
        }
        return null;
    }
}
