package fr.free.nrw.commons.review

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ActivityReviewBinding
import fr.free.nrw.commons.media.MediaDetailFragment
import fr.free.nrw.commons.utils.DialogUtil
import java.util.Locale

/** Owns review explanations and the presentation of the selected image's details. */
internal class ReviewScreenUi(
    private val activity: FragmentActivity,
    private val binding: ActivityReviewBinding,
) {
    private var mediaDetailFragment: MediaDetailFragment? = null

    /** Shows the explanation for skipping an image. */
    fun showSkipImageInfo() {
        DialogUtil.showAlertDialog(
            activity,
            activity.getString(R.string.skip_image).uppercase(Locale.ROOT),
            activity.getString(R.string.skip_image_explanation),
            activity.getString(R.string.ok),
            null,
            null,
            null,
        )
    }

    /** Shows the explanation for reviewing an image. */
    fun showReviewImageInfo() {
        DialogUtil.showAlertDialog(
            activity,
            activity.getString(R.string.title_activity_review),
            activity.getString(R.string.review_image_explanation),
            activity.getString(R.string.ok),
            null,
            null,
            null,
        )
    }

    /**
     * this function return the instance of  reviewImageFragment
     */
    fun getInstanceOfReviewImageFragment(reviewPagerAdapter: ReviewPagerAdapter): ReviewImageFragment? {
        val currentItemOfReviewPager = binding.viewPagerReview.currentItem
        return reviewPagerAdapter.instantiateItem(
            binding.viewPagerReview,
            currentItemOfReviewPager,
        ) as? ReviewImageFragment
    }

    /**
     * set up the media detail fragment when click on the review image
     */
    fun setUpMediaDetailFragment(media: Media?) {
        if (binding.mediaDetailContainer.visibility == View.GONE && media != null) {
            binding.mediaDetailContainer.visibility = View.VISIBLE
            binding.reviewActivityContainer.visibility = View.INVISIBLE
            val fragmentManager = activity.supportFragmentManager
            mediaDetailFragment =
                MediaDetailFragment().apply {
                    arguments =
                        Bundle().apply {
                            putParcelable("media", media)
                        }
                }
            fragmentManager
                .beginTransaction()
                .add(R.id.mediaDetailContainer, mediaDetailFragment!!)
                .addToBackStack("MediaDetail")
                .commit()
        }
    }

    /**
     * set up media detail fragment after orientation change
     */
    fun setUpMediaDetailOnOrientation() {
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.mediaDetailContainer)
        fragment?.let {
            binding.mediaDetailContainer.visibility = View.VISIBLE
            binding.reviewActivityContainer.visibility = View.INVISIBLE
            activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.mediaDetailContainer, it)
                .commit()
        }
    }
}
