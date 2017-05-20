package fr.free.nrw.commons;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.facebook.drawee.view.SimpleDraweeView;

public class MediaWikiImageView extends SimpleDraweeView {
    private ThumbnailFetchTask currentThumbnailTask;

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
        if(media == null) {
            return;
        }

        if (CommonsApplication.getInstance().getThumbnailUrlCache().get(media.getFilename()) != null) {
            setImageUrl(CommonsApplication.getInstance().getThumbnailUrlCache().get(media.getFilename()));
        } else {
            setImageUrl(null);
            currentThumbnailTask = new ThumbnailFetchTask(media);
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
        ThumbnailFetchTask(@NonNull Media media) {
            super(media);
        }

        @Override
        protected void onPostExecute(String result) {
            if (isCancelled()) {
                return;
            }
            if (TextUtils.isEmpty(result) && media.getLocalUri() != null) {
                result = media.getLocalUri().toString();
            } else {
                // only cache meaningful thumbnails received from network.
                CommonsApplication.getInstance().getThumbnailUrlCache().put(media.getFilename(), result);
            }
            setImageUrl(result);
        }
    }
}
