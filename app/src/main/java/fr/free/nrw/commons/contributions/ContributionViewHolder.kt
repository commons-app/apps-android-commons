package fr.free.nrw.commons.contributions

import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.LayoutContributionBinding
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File

class ContributionViewHolder internal constructor(
    private val parent: View, private val callback: ContributionsListAdapter.Callback,
    private val mediaClient: MediaClient
) : RecyclerView.ViewHolder(parent) {
    var binding: LayoutContributionBinding = LayoutContributionBinding.bind(parent)

    private var position = 0
    private var contribution: Contribution? = null
    private val compositeDisposable = CompositeDisposable()
    private var isWikipediaButtonDisplayed = false
    private val pausingPopUp: AlertDialog
    var imageRequest: ImageRequest? = null
        private set

    init {
        binding.contributionImage.setOnClickListener { v: View? -> imageClicked() }
        binding.wikipediaButton.setOnClickListener { v: View? -> wikipediaButtonClicked() }

        /* Set a dialog indicating that the upload is being paused. This is needed because pausing
an upload might take a dozen seconds. */
        val builder = AlertDialog.Builder(
            parent.context
        )
        builder.setCancelable(false)
        builder.setView(R.layout.progress_dialog)
        pausingPopUp = builder.create()
    }

    fun init(position: Int, contribution: Contribution?) {
        //handling crashes when the contribution is null.

        if (null == contribution) {
            return
        }

        this.contribution = contribution
        this.position = position
        binding.contributionTitle.text = contribution.media.mostRelevantCaption
        binding.authorView.text = contribution.media.getAuthorOrUser()

        //Removes flicker of loading image.
        binding.contributionImage.hierarchy.fadeDuration = 0

        binding.contributionImage.hierarchy.setPlaceholderImage(R.drawable.image_placeholder)
        binding.contributionImage.hierarchy.setFailureImage(R.drawable.image_placeholder)

        val imageSource = chooseImageSource(
            contribution.media.thumbUrl,
            contribution.localUri
        )
        if (!TextUtils.isEmpty(imageSource)) {
            if (URLUtil.isHttpsUrl(imageSource)) {
                imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageSource))
                    .setProgressiveRenderingEnabled(true)
                    .build()
            } else if (URLUtil.isFileUrl(imageSource)) {
                imageRequest = ImageRequest.fromUri(Uri.parse(imageSource))
            } else if (imageSource != null) {
                val file = File(imageSource)
                imageRequest = ImageRequest.fromFile(file)
            }

            if (imageRequest != null) {
                binding.contributionImage.setImageRequest(imageRequest)
            }
        }

        binding.contributionSequenceNumber.text = (position + 1).toString()
        binding.contributionSequenceNumber.visibility = View.VISIBLE
        binding.wikipediaButton.visibility = View.GONE
        binding.contributionState.visibility = View.GONE
        binding.contributionProgress.visibility = View.GONE
        binding.imageOptions.visibility = View.GONE
        binding.contributionState.text = ""
        checkIfMediaExistsOnWikipediaPage(contribution)
    }

    /**
     * Checks if a media exists on the corresponding Wikipedia article Currently the check is made
     * for the device's current language Wikipedia
     *
     * @param contribution
     */
    private fun checkIfMediaExistsOnWikipediaPage(contribution: Contribution) {
        if (contribution.wikidataPlace == null
            || contribution.wikidataPlace!!.wikipediaArticle == null
        ) {
            return
        }
        val wikipediaArticle = contribution.wikidataPlace!!.getWikipediaPageTitle()
        compositeDisposable.add(
            mediaClient.doesPageContainMedia(wikipediaArticle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { mediaExists: Boolean ->
                    displayWikipediaButton(mediaExists)
                })
    }

    /**
     * Handle action buttons visibility if the corresponding wikipedia page doesn't contain any
     * media. This method needs to control the state of just the scenario where media does not
     * exists as other scenarios are already handled in the init method.
     *
     * @param mediaExists
     */
    private fun displayWikipediaButton(mediaExists: Boolean) {
        if (!mediaExists) {
            binding.wikipediaButton.visibility = View.VISIBLE
            isWikipediaButtonDisplayed = true
            binding.imageOptions.visibility = View.VISIBLE
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
    private fun chooseImageSource(thumbUrl: String?, localUri: Uri?): String? {
        return if (!TextUtils.isEmpty(thumbUrl)) thumbUrl else localUri?.toString()
    }

    fun imageClicked() {
        callback.openMediaDetail(position, isWikipediaButtonDisplayed)
    }

    fun wikipediaButtonClicked() {
        callback.addImageToWikipedia(contribution)
    }
}
