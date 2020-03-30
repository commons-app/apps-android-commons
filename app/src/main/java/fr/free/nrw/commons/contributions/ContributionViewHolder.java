package fr.free.nrw.commons.contributions;

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

import com.facebook.drawee.view.SimpleDraweeView;

import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsListAdapter.Callback;
import java.util.HashMap;
import java.util.Random;

public class ContributionViewHolder extends RecyclerView.ViewHolder {

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

    ContributionViewHolder(View parent, Callback callback) {
        super(parent);
        ButterKnife.bind(this, parent);
        this.callback=callback;
    }

    public void init(int position, Contribution contribution) {
        this.contribution = contribution;
        this.position = position;
        String imageSource = chooseImageSource(contribution.thumbUrl, contribution.getLocalUri());
        if (!TextUtils.isEmpty(imageSource)) {
            final ImageRequest imageRequest =
                ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageSource))
                    .setProgressiveRenderingEnabled(true)
                    .build();
            imageView.setImageRequest(imageRequest);
        }
        titleView.setText(contribution.getDisplayTitle());

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
