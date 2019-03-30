package fr.free.nrw.commons;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class MediaWikiImageView extends SimpleDraweeView {
    @Inject MediaWikiApi mwApi;
    @Inject LruCache<String, String> thumbnailUrlCache;

    protected CompositeDisposable compositeDisposable = new CompositeDisposable();

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
        if (media == null) {
            return;
        }

        Disposable disposable = fetchMediaThumbnail(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(thumbnail -> {
                    if (!StringUtils.isBlank(thumbnail)) {
                        setImageUrl(thumbnail);
                    }
                }, throwable -> Timber.e(throwable, "Error occurred while fetching thumbnail"));

        compositeDisposable.add(disposable);
    }

    @Override
    protected void onDetachedFromWindow() {
        compositeDisposable.clear();
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

    //TODO: refactor the logic for thumbnails. ImageInfo API can be used to fetch thumbnail upfront

    /**
     * Fetches media thumbnail from the server
     *
     * @param media
     * @return
     */
    public Single<String> fetchMediaThumbnail(Media media) {
        if (media.getFilename() != null && thumbnailUrlCache.get(media.getFilename()) != null) {
            return Single.just(thumbnailUrlCache.get(media.getFilename()));
        }
        return mwApi.findThumbnailByFilename(media.getFilename())
                .map(result -> {
                    if (TextUtils.isEmpty(result) && media.getLocalUri() != null) {
                        return media.getLocalUri().toString();
                    } else {
                        thumbnailUrlCache.put(media.getFilename(), result);
                        return result;
                    }
                });
    }

    /**
     * Displays the image from the URL.
     * @param url the URL of the image
     */
    private void setImageUrl(@Nullable String url) {
        setImageURI(url);
    }

}
