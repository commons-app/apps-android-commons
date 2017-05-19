package fr.free.nrw.commons;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.util.AttributeSet;

import com.facebook.drawee.view.SimpleDraweeView;

public class MediaWikiImageView extends SimpleDraweeView {
    private ThumbnailFetchTask currentThumbnailTask;
    LruCache<String, String> thumbnailUrlCache = new LruCache<>(1024);

    public MediaWikiImageView(Context context) {
        this(context, null);
    }

    public MediaWikiImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaWikiImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMedia(Media media) {
        if (currentThumbnailTask != null) {
            currentThumbnailTask.cancel(true);
        }
        setImageURI((String) null);
        if(media == null) {
            return;
        }

        if (thumbnailUrlCache.get(media.getFilename()) != null) {
            setImageUrl(thumbnailUrlCache.get(media.getFilename()));
        } else {
            currentThumbnailTask = new ThumbnailFetchTask();
            currentThumbnailTask.execute(media.getFilename());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (currentThumbnailTask != null) {
            currentThumbnailTask.cancel(true);
        }
        super.onDetachedFromWindow();
    }

    private void setImageUrl(@Nullable String url) {
        setImageURI(url);
    }

    private class ThumbnailFetchTask extends MediaThumbnailFetchTask {
        @Override
        protected void onPostExecute(String result) {
            if (isCancelled()) {
                return;
            }
            setImageUrl(result);
        }
    }
}
