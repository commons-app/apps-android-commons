package fr.free.nrw.commons;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Toast;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import timber.log.Timber;

public class MediaWikiImageView extends SimpleDraweeView {
    private ThumbnailFetchTask currentThumbnailTask;

    public MediaWikiImageView(Context context) {
        this(context, null);
        init();
    }

    public MediaWikiImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public MediaWikiImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setMedia(Media media) {
        if (currentThumbnailTask != null) {
            currentThumbnailTask.cancel(true);
        }
        if (media == null) {
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

    private void init() {
        setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp, getContext().getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp, getContext().getTheme()))
                .build());
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
                try {
                    CommonsApplication.getInstance().getThumbnailUrlCache().put(media.getFilename(), result);
                } catch (NullPointerException npe) {
                    Timber.e("error when adding pic to cache " + npe);

                    Toast.makeText(getContext(), R.string.error_while_cache, Toast.LENGTH_SHORT).show();
                }
            }
            setImageUrl(result);
        }
    }
}
