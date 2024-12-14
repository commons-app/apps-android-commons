package fr.free.nrw.commons.contributions;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsListAdapter.Callback;
import fr.free.nrw.commons.databinding.LayoutContributionBinding;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;

public class ContributionViewHolder extends RecyclerView.ViewHolder {

    private final Callback callback;

    LayoutContributionBinding binding;

    private int position;
    private Contribution contribution;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final MediaClient mediaClient;
    private boolean isWikipediaButtonDisplayed;
    private AlertDialog pausingPopUp;
    private View parent;
    private ImageRequest imageRequest;

    ContributionViewHolder(final View parent, final Callback callback,
        final MediaClient mediaClient) {
        super(parent);
        this.parent = parent;
        this.mediaClient = mediaClient;
        this.callback = callback;

        binding = LayoutContributionBinding.bind(parent);

        binding.contributionImage.setOnClickListener(v -> imageClicked());
        binding.wikipediaButton.setOnClickListener(v -> wikipediaButtonClicked());

        /* Set a dialog indicating that the upload is being paused. This is needed because pausing
        an upload might take a dozen seconds. */
        AlertDialog.Builder builder = new Builder(parent.getContext());
        builder.setCancelable(false);
        builder.setView(R.layout.progress_dialog);
        pausingPopUp = builder.create();
    }

    public void init(final int position, final Contribution contribution) {

        //handling crashes when the contribution is null.
        if (null == contribution) {
            return;
        }

        this.contribution = contribution;
        this.position = position;
        binding.contributionTitle.setText(contribution.getMedia().getMostRelevantCaption());
        binding.authorView.setText(contribution.getMedia().getAuthor());

        //Removes flicker of loading image.
        binding.contributionImage.getHierarchy().setFadeDuration(0);

        binding.contributionImage.getHierarchy().setPlaceholderImage(R.drawable.image_placeholder);
        binding.contributionImage.getHierarchy().setFailureImage(R.drawable.image_placeholder);

        final String imageSource = chooseImageSource(contribution.getMedia().getThumbUrl(),
            contribution.getLocalUri());
        if (!TextUtils.isEmpty(imageSource)) {
            if (URLUtil.isHttpsUrl(imageSource)) {
                imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageSource))
                    .setProgressiveRenderingEnabled(true)
                    .build();
            } else if (URLUtil.isFileUrl(imageSource)) {
                imageRequest = ImageRequest.fromUri(Uri.parse(imageSource));
            } else if (imageSource != null) {
                final File file = new File(imageSource);
                imageRequest = ImageRequest.fromFile(file);
            }

            if (imageRequest != null) {
                binding.contributionImage.setImageRequest(imageRequest);
            }
        }

        binding.contributionSequenceNumber.setText(String.valueOf(position + 1));
        binding.contributionSequenceNumber.setVisibility(View.VISIBLE);
        binding.wikipediaButton.setVisibility(View.GONE);
        binding.contributionState.setVisibility(View.GONE);
        binding.contributionProgress.setVisibility(View.GONE);
        binding.imageOptions.setVisibility(View.GONE);
        binding.contributionState.setText("");
        checkIfMediaExistsOnWikipediaPage(contribution);

    }

    /**
     * Checks if a media exists on the corresponding Wikipedia article Currently the check is made
     * for the device's current language Wikipedia
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
     * Handle action buttons visibility if the corresponding wikipedia page doesn't contain any
     * media. This method needs to control the state of just the scenario where media does not
     * exists as other scenarios are already handled in the init method.
     *
     * @param mediaExists
     */
    private void displayWikipediaButton(Boolean mediaExists) {
        if (!mediaExists) {
            binding.wikipediaButton.setVisibility(View.VISIBLE);
            isWikipediaButtonDisplayed = true;
            binding.imageOptions.setVisibility(View.VISIBLE);
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

    public void imageClicked() {
        callback.openMediaDetail(position, isWikipediaButtonDisplayed);
    }

    public void wikipediaButtonClicked() {
        callback.addImageToWikipedia(contribution);
    }

    public ImageRequest getImageRequest() {
        return imageRequest;
    }
}
