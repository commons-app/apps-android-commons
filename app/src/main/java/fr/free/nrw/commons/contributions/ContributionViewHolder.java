package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsListAdapter.Callback;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
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
    @BindView(R.id.image_options) LinearLayout imageOptions;
    @BindView(R.id.wikipediaButton) ImageButton addToWikipediaButton;


    private int position;
    private Contribution contribution;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MediaClient mediaClient;

    ContributionViewHolder(final View parent, final Callback callback,
        final MediaClient mediaClient) {
        super(parent);
        this.mediaClient = mediaClient;
        ButterKnife.bind(this, parent);
        this.callback=callback;
    }

    public void init(final int position, final Contribution contribution) {
        this.contribution = contribution;
        fetchAndDisplayCaption(contribution);
        this.position = position;
        final String imageSource = chooseImageSource(contribution.getThumbUrl(), contribution.getLocalUri());
        if (!TextUtils.isEmpty(imageSource)) {
            final ImageRequest imageRequest =
                ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageSource))
                    .setProgressiveRenderingEnabled(true)
                    .build();
            imageView.setImageRequest(imageRequest);
        }

        seqNumView.setText(String.valueOf(position + 1));
        seqNumView.setVisibility(View.VISIBLE);

        switch (contribution.getState()) {
            case Contribution.STATE_COMPLETED:
                stateView.setVisibility(View.GONE);
                progressView.setVisibility(View.GONE);
                imageOptions.setVisibility(View.GONE);
                stateView.setText("");
                break;
            case Contribution.STATE_QUEUED:
                stateView.setVisibility(View.VISIBLE);
                progressView.setVisibility(View.GONE);
                stateView.setText(R.string.contribution_state_queued);
                imageOptions.setVisibility(View.GONE);
                break;
            case Contribution.STATE_IN_PROGRESS:
                stateView.setVisibility(View.GONE);
                progressView.setVisibility(View.VISIBLE);
                imageOptions.setVisibility(View.GONE);
                final long total = contribution.getDataLength();
                final long transferred = contribution.getTransferred();
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
                imageOptions.setVisibility(View.VISIBLE);
                break;
        }

        checkIfMediaExistsOnWikipediaPage(contribution);
    }

    /**
     * In contributions first we show the title for the image stored in cache,
     * then we fetch captions associated with the image and replace title on the thumbnail with caption
     *
     * @param contribution
     */
    private void fetchAndDisplayCaption(final Contribution contribution) {
        if ((contribution.getState() != Contribution.STATE_COMPLETED)) {
            titleView.setText(contribution.getDisplayTitle());
        } else {
            final String pageId = contribution.getPageId();
            if (pageId != null) {
                Timber.d("Fetching caption for %s", contribution.getFilename());
                final String wikibaseMediaId = PAGE_ID_PREFIX
                    + pageId; // Create Wikibase media id from the page id. Example media id: M80618155 for https://commons.wikimedia.org/wiki/File:Tantanmen.jpeg with has the pageid 80618155
                compositeDisposable.add(mediaClient.getCaptionByWikibaseIdentifier(wikibaseMediaId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subscriber -> {
                        if (!subscriber.trim().equals(MediaClient.NO_CAPTION)) {
                            titleView.setText(subscriber);
                        } else {
                            titleView.setText(contribution.getDisplayTitle());
                        }
                    }));
            } else {
                titleView.setText(contribution.getDisplayTitle());
            }
        }
    }

    /**
     * Checks if a media exists on the corresponding Wikipedia article
     * Currently the check is made for the device's current language Wikipedia
     * @param contribution
     */
    private void checkIfMediaExistsOnWikipediaPage(final Contribution contribution) {
        final String wikipediaArticle = contribution.getWikidataPlace().getWikipediaArticle();
        compositeDisposable.add(mediaClient.doesPageContainMedia(wikipediaArticle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(mediaExists -> {
                addToWikipediaButton.setVisibility(mediaExists? View.GONE: View.VISIBLE);
            }));
    }

    /**
     * Returns the image source for the image view, first preference is given to thumbUrl if that is
     * null, moves to local uri and if both are null return null
     *
     * @param thumbUrl
     * @param localUri
     * @return
     */
    @Nullable
    private String chooseImageSource(final String thumbUrl, final Uri localUri) {
        return !TextUtils.isEmpty(thumbUrl) ? thumbUrl :
            localUri != null ? localUri.toString() :
                null;
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

    @OnClick(R.id.wikipediaButton)
    public void wikipediaButtonClicked(){
        callback.openMediaDetail(position);
    }
}
