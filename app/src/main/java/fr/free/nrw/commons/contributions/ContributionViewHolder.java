package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ViewHolder;
import fr.free.nrw.commons.contributions.model.DisplayableContribution;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ContributionViewHolder implements ViewHolder<DisplayableContribution> {
    @BindView(R.id.contributionImage)
    SimpleDraweeView imageView;
    @BindView(R.id.contributionTitle) TextView titleView;
    @BindView(R.id.contributionState) TextView stateView;
    @BindView(R.id.contributionSequenceNumber) TextView seqNumView;
    @BindView(R.id.contributionProgress) ProgressBar progressView;
    @BindView(R.id.failed_image_options) LinearLayout failedImageOptions;

    @Inject
    MediaDataExtractor mediaDataExtractor;

    private DisplayableContribution contribution;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    ContributionViewHolder(View parent) {
        ButterKnife.bind(this, parent);
    }

    @Override
    public void bindModel(Context context, DisplayableContribution contribution) {
        ApplicationlessInjection.getInstance(context)
                .getCommonsApplicationComponent().inject(this);
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
     * This can be removed once #2904 is in place and contribution contains all metadata beforehand
     * @param contribution
     */
    private void fetchAndDisplayThumbnail(DisplayableContribution contribution) {
        Timber.d("Fetching thumbnail for %s", contribution.getFilename());
        Disposable disposable = mediaDataExtractor.getMediaFromFileName(contribution.getFilename())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(media -> {
                    imageView.setImageURI(media.getThumbUrl());
                });
        compositeDisposable.add(disposable);
    }

    public void clear() {
        compositeDisposable.clear();
    }

    /**
     * Retry upload when it is failed
     */
    @OnClick(R.id.retryButton)
    public void retryUpload() {
        DisplayableContribution.ContributionActions actions = contribution.getContributionActions();
        if (actions != null) {
            actions.retryUpload();
        }
    }

    /**
     * Delete a failed upload attempt
     */
    @OnClick(R.id.cancelButton)
    public void deleteUpload() {
        DisplayableContribution.ContributionActions actions = contribution.getContributionActions();
        if (actions != null) {
            actions.deleteUpload();
        }
    }

    @OnClick(R.id.contributionImage)
    public void imageClicked(){
        DisplayableContribution.ContributionActions actions = contribution.getContributionActions();
        if (actions != null) {
            actions.onClick();
        }
    }
}
