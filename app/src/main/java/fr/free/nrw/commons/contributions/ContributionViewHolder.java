package fr.free.nrw.commons.contributions;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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

public class ContributionViewHolder extends RecyclerView.ViewHolder {

  private final Callback callback;
  @BindView(R.id.contributionImage)
  SimpleDraweeView imageView;
  @BindView(R.id.contributionTitle)
  TextView titleView;
  @BindView(R.id.contributionState)
  TextView stateView;
  @BindView(R.id.contributionSequenceNumber)
  TextView seqNumView;
  @BindView(R.id.contributionProgress)
  ProgressBar progressView;
  @BindView(R.id.image_options)
  RelativeLayout imageOptions;
  @BindView(R.id.wikipediaButton)
  ImageButton addToWikipediaButton;
  @BindView(R.id.retryButton)
  ImageButton retryButton;
  @BindView(R.id.cancelButton)
  ImageButton cancelButton;
  @BindView(R.id.pauseResumeButton)
  ImageButton pauseResumeButton;


  private int position;
  private Contribution contribution;
  private final CompositeDisposable compositeDisposable = new CompositeDisposable();
  private final MediaClient mediaClient;
  private boolean isWikipediaButtonDisplayed;

  ContributionViewHolder(final View parent, final Callback callback,
      final MediaClient mediaClient) {
    super(parent);
    this.mediaClient = mediaClient;
    ButterKnife.bind(this, parent);
    this.callback = callback;
  }

  public void init(final int position, final Contribution contribution) {
    this.contribution = contribution;
    this.position = position;
    titleView.setText(contribution.getMedia().getMostRelevantCaption());
    final String imageSource = chooseImageSource(contribution.getMedia().getThumbUrl(),
        contribution.getLocalUri());
    if (!TextUtils.isEmpty(imageSource)) {
      final ImageRequest imageRequest =
          ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageSource))
              .setProgressiveRenderingEnabled(true)
              .build();
      imageView.setImageRequest(imageRequest);
    }

    seqNumView.setText(String.valueOf(position + 1));
    seqNumView.setVisibility(View.VISIBLE);

    addToWikipediaButton.setVisibility(View.GONE);
    switch (contribution.getState()) {
      case Contribution.STATE_COMPLETED:
        stateView.setVisibility(View.GONE);
        progressView.setVisibility(View.GONE);
        imageOptions.setVisibility(View.GONE);
        stateView.setText("");
        checkIfMediaExistsOnWikipediaPage(contribution);
        break;
      case Contribution.STATE_QUEUED:
      case Contribution.STATE_QUEUED_LIMITED_CONNECTION_MODE:
        stateView.setVisibility(View.VISIBLE);
        progressView.setVisibility(View.GONE);
        stateView.setText(R.string.contribution_state_queued);
        imageOptions.setVisibility(View.GONE);
        break;
      case Contribution.STATE_IN_PROGRESS:
        stateView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
        addToWikipediaButton.setVisibility(View.GONE);
        pauseResumeButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        imageOptions.setVisibility(View.VISIBLE);
        final long total = contribution.getDataLength();
        final long transferred = contribution.getTransferred();
        if (transferred == 0 || transferred >= total) {
          progressView.setIndeterminate(true);
        } else {
          progressView.setProgress((int) (((double) transferred / (double) total) * 100));
        }
        break;
      case Contribution.STATE_PAUSED:
        stateView.setVisibility(View.VISIBLE);
        stateView.setText(R.string.paused);
        setResume();
        progressView.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        pauseResumeButton.setVisibility(View.VISIBLE);
        imageOptions.setVisibility(View.VISIBLE);
        break;
      case Contribution.STATE_FAILED:
        stateView.setVisibility(View.VISIBLE);
        stateView.setText(R.string.contribution_state_failed);
        progressView.setVisibility(View.GONE);
        cancelButton.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        pauseResumeButton.setVisibility(View.GONE);
        imageOptions.setVisibility(View.VISIBLE);
        break;
    }
  }

  /**
   * Checks if a media exists on the corresponding Wikipedia article Currently the check is made for
   * the device's current language Wikipedia
   *
   * @param contribution
   */
  private void checkIfMediaExistsOnWikipediaPage(final Contribution contribution) {
    if (contribution.getWikidataPlace() == null
        || contribution.getWikidataPlace().getWikipediaArticle() == null) {
      return;
    }
    final String wikipediaArticle = contribution.getWikidataPlace().getWikipediaPageTitle();
    compositeDisposable.add(mediaClient.doesPageContainMedia(wikipediaArticle)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(mediaExists -> {
          displayWikipediaButton(mediaExists);
        }));
  }

  /**
   * Handle action buttons visibility if the corresponding wikipedia page doesn't contain any media.
   * This method needs to control the state of just the scenario where media does not exists as
   * other scenarios are already handled in the init method.
   *
   * @param mediaExists
   */
  private void displayWikipediaButton(Boolean mediaExists) {
    if (!mediaExists) {
      addToWikipediaButton.setVisibility(View.VISIBLE);
      isWikipediaButtonDisplayed = true;
      cancelButton.setVisibility(View.GONE);
      retryButton.setVisibility(View.GONE);
      imageOptions.setVisibility(View.VISIBLE);
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
  public void imageClicked() {
    callback.openMediaDetail(position, isWikipediaButtonDisplayed);
  }

  @OnClick(R.id.wikipediaButton)
  public void wikipediaButtonClicked() {
    callback.addImageToWikipedia(contribution);
  }

  /**
   * Triggers a callback for pause/resume
   */
  @OnClick(R.id.pauseResumeButton)
  public void onPauseResumeButtonClicked() {
    if (pauseResumeButton.getTag().toString().equals("pause")) {
      callback.pauseUpload(contribution);
      setResume();
    } else {
      callback.resumeUpload(contribution);
      setPaused();
    }
  }

  /**
   * Update pause/resume button to show pause state
   */
  private void setPaused() {
    pauseResumeButton.setImageResource(R.drawable.pause_icon);
    pauseResumeButton.setTag(R.string.pause);
  }

  /**
   * Update pause/resume button to show resume state
   */
  private void setResume() {
    pauseResumeButton.setImageResource(R.drawable.play_icon);
    pauseResumeButton.setTag(R.string.resume);
  }
}
