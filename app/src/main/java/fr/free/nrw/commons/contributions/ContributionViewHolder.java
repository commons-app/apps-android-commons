package fr.free.nrw.commons.contributions;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.collection.LruCache;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsListAdapter.Callback;
import fr.free.nrw.commons.contributions.model.DisplayableContribution;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.upload.FileUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ContributionViewHolder extends RecyclerView.ViewHolder {

    private final Callback callback;
    @BindView(R.id.contributionImage)
    SimpleDraweeView imageView;
    @BindView(R.id.contributionTitle) TextView titleView;
    @BindView(R.id.contributionState) TextView stateView;
    @BindView(R.id.contributionSequenceNumber) TextView seqNumView;
    @BindView(R.id.contributionProgress) ProgressBar progressView;
    @BindView(R.id.failed_image_options) LinearLayout failedImageOptions;

    @Inject
    MediaDataExtractor mediaDataExtractor;

    @Inject
    @Named("thumbnail-cache")
    LruCache<String, String> thumbnailCache;

    private DisplayableContribution contribution;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private int position;

    ContributionViewHolder(View parent, Callback callback) {
        super(parent);
        ButterKnife.bind(this, parent);
        this.callback=callback;
    }

    public void init(int position, DisplayableContribution contribution) {
        ApplicationlessInjection.getInstance(itemView.getContext())
                .getCommonsApplicationComponent().inject(this);
        this.position=position;
        this.contribution = contribution;
        fetchAndDisplayThumbnail(contribution);
        titleView.setText(contribution.getDisplayTitle());

        seqNumView.setText(String.valueOf(contribution.getPosition() + 1));
        seqNumView.setVisibility(View.VISIBLE);

        switch (contribution.getState()) {
            case Contribution.STATE_COMPLETED:
                stateView.setVisibility(View.GONE);
                progressView.setVisibility(View.GONE);
                failedImageOptions.setVisibility(View.GONE);
                stateView.setText("");
                break;
            case Contribution.STATE_QUEUED:
                stateView.setVisibility(View.VISIBLE);
                progressView.setVisibility(View.GONE);
                stateView.setText(R.string.contribution_state_queued);
                failedImageOptions.setVisibility(View.GONE);
                break;
            case Contribution.STATE_IN_PROGRESS:
                stateView.setVisibility(View.GONE);
                progressView.setVisibility(View.VISIBLE);
                failedImageOptions.setVisibility(View.GONE);
                long total = contribution.getDataLength();
                long transferred = contribution.getTransferred();
                if (transferred == 0 || transferred >= total) {
                    progressView.setIndeterminate(true);
                } else {
                    progressView.setProgress((int)(((double)transferred / (double)total) * 100));
                }
                break;
            case Contribution.STATE_FAILED:
                stateView.setVisibility(View.VISIBLE);
                stateView.setText(R.string.contribution_state_failed);
                progressView.setVisibility(View.GONE);
                failedImageOptions.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * This method fetches the thumbnail url from file name
     * If the thumbnail url is present in cache, then it is used otherwise API call is made to fetch the thumbnail
     * This can be removed once #2904 is in place and contribution contains all metadata beforehand
     * @param contribution
     */
    private void fetchAndDisplayThumbnail(DisplayableContribution contribution) {
        String keyForLRUCache = contribution.getFilename();
        String cacheUrl = thumbnailCache.get(keyForLRUCache);
        if (!StringUtils.isBlank(cacheUrl)) {
            imageView.setImageURI(cacheUrl);
            return;
        }

        imageView.setBackground(null);
        if ((contribution.getState() != Contribution.STATE_COMPLETED) && FileUtils.fileExists(
                contribution.getLocalUri())) {
            imageView.setImageURI(contribution.getLocalUri());
        } else {
            Timber.d("Fetching thumbnail for %s", contribution.getFilename());
            Disposable disposable = mediaDataExtractor
                    .getMediaFromFileName(contribution.getFilename())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(media -> {
                        thumbnailCache.put(keyForLRUCache, media.getThumbUrl());
                        imageView.setImageURI(media.getThumbUrl());
                    });
            compositeDisposable.add(disposable);
        }

    }

    public void clear() {
        compositeDisposable.clear();
    }

    /**
     * Retry upload when it is failed
     */
    @OnClick(R.id.retryButton)
    public void retryUpload() {
        callback.retryUpload(contribution);
    }

    /**
     * Delete a failed upload attempt
     */
    @OnClick(R.id.cancelButton)
    public void deleteUpload() {
        callback.deleteUpload(contribution);
    }

    @OnClick(R.id.contributionImage)
    public void imageClicked(){
        callback.openMediaDetail(position);
    }
}
