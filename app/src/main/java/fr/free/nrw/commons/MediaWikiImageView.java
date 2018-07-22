package fr.free.nrw.commons;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Toast;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import javax.inject.Inject;

import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

public class MediaWikiImageView extends SimpleDraweeView {
    @Inject MediaWikiApi mwApi;
    @Inject LruCache<String, String> thumbnailUrlCache;

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

    /**
     * Sets the media. Fetches its thumbnail if necessary.
     * @param media the new media
     */
    public void setMedia(Media media) {
        if (currentThumbnailTask != null) {
            currentThumbnailTask.cancel(true);
        }
        if (media == null) {
            return;
        }

        if(media.getFilename() != null) {
            if (thumbnailUrlCache.get(media.getFilename()) != null) {
                setImageUrl(thumbnailUrlCache.get(media.getFilename()));
            } else {
                setImageUrl(null);
                currentThumbnailTask = new ThumbnailFetchTask(media, mwApi);
                currentThumbnailTask.execute(media.getFilename());
            }
        } else { // local image
            setImageUrl(media.getLocalUri().toString());
            currentThumbnailTask = new ThumbnailFetchTask(media, mwApi);
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

    /**
     * Initializes MediaWikiImageView.
     */
    private void init() {
        ApplicationlessInjection
                .getInstance(getContext()
                        .getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);
        setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp, getContext().getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp, getContext().getTheme()))
                .build());
    }

    /**
     * Displays the image from the URL.
     * @param url the URL of the image
     */
    private void setImageUrl(@Nullable String url) {
        setImageURI(url);
    }

    private class ThumbnailFetchTask extends MediaThumbnailFetchTask {
        ThumbnailFetchTask(@NonNull Media media, @NonNull MediaWikiApi mwApi) {
            super(media, mwApi);
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
                    thumbnailUrlCache.put(media.getFilename(), result);
                } catch (NullPointerException npe) {
                    Timber.e("error when adding pic to cache " + npe);

                    Toast.makeText(getContext(), R.string.error_while_cache, Toast.LENGTH_SHORT).show();
                }
            }
            setImageUrl(result);
        }
    }
}
