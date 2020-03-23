package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public class ContributionViewHolder extends RecyclerView.ViewHolder {

    private static final long TIMEOUT_SECONDS = 15;
    private static final String NO_CAPTION = "No caption";
    private final Callback callback;
    @BindView(R.id.contributionImage)
    SimpleDraweeView imageView;
    @BindView(R.id.contributionTitle) TextView titleView;
    @BindView(R.id.contributionState) TextView stateView;
    @BindView(R.id.contributionSequenceNumber) TextView seqNumView;
    @BindView(R.id.contributionProgress) ProgressBar progressView;
    @BindView(R.id.failed_image_options) LinearLayout failedImageOptions;


    private int position;
    private Contribution contribution;
    private Random random = new Random();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MediaClient mediaClient;

    ContributionViewHolder(View parent, Callback callback,
        MediaClient mediaClient) {
        super(parent);
        this.mediaClient = mediaClient;
        ButterKnife.bind(this, parent);
        this.callback=callback;
    }

    public void init(int position, Contribution contribution) {
        this.contribution = contribution;
        fetchAndDisplayCaption(contribution);
        this.position = position;
        imageView.getHierarchy().setPlaceholderImage(new ColorDrawable(
            Color.argb(100, random.nextInt(256), random.nextInt(256), random.nextInt(256))));
        String imageSource = chooseImageSource(contribution.thumbUrl, contribution.getLocalUri());
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
     * In contributions first we show the title for the image stored in cache,
     * then we fetch captions associated with the image and replace title on the thumbnail with caption
     *
     * @param contribution
     */
    private void fetchAndDisplayCaption(Contribution contribution) {
        if ((contribution.getState() != Contribution.STATE_COMPLETED)) {
            titleView.setText(contribution.getDisplayTitle());
        } else {
            Timber.d("Fetching caption for %s", contribution.getFilename());
            String wikibaseMediaId = PAGE_ID_PREFIX + contribution.getPageId(); // Create Wikibase media id from the page id. Example media id: M80618155 for https://commons.wikimedia.org/wiki/File:Tantanmen.jpeg with has the pageid 80618155
            compositeDisposable.add(mediaClient.getCaptionByWikibaseIdentifier(wikibaseMediaId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .subscribe(subscriber -> {
                        if (!subscriber.trim().equals(NO_CAPTION)) {
                            titleView.setText(subscriber);
                        } else titleView.setText(contribution.getDisplayTitle());
                    }));
        }
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
    private String chooseImageSource(String thumbUrl, Uri localUri) {
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
}
