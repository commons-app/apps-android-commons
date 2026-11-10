package fr.free.nrw.commons.upload

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import fr.free.nrw.commons.upload.ThumbnailsAdapter.OnThumbnailDeletedListener
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment

/** Controls upload pages and their thumbnail strip. */
internal class UploadNavigation :
    UploadBaseFragment.Callback,
    OnThumbnailDeletedListener {
    lateinit var view: UploadActivityView
    private val activity get() = view.activity

    override val totalNumberOfSteps get() = view.fragments!!.size
    override val isWLMUpload get() = view.place != null && view.place!!.isMonument

    /** Initializes the thumbnail strip and deletion listener. */
    fun initThumbnailsRecyclerView() {
        view.binding.rvThumbnails.layoutManager =
            LinearLayoutManager(
                activity,
                LinearLayoutManager.HORIZONTAL,
                false,
            )
        view.thumbnailsAdapter = ThumbnailsAdapter { view.currentSelectedPosition }
        view.thumbnailsAdapter!!.onThumbnailDeletedListener = activity
        view.binding.rvThumbnails.adapter = view.thumbnailsAdapter
    }

    /** Initializes upload pages and thumbnail selection updates. */
    fun initViewPager() {
        view.uploadImagesAdapter = UploadImageAdapter(activity.supportFragmentManager)
        view.binding.vpUpload.adapter = view.uploadImagesAdapter
        view.binding.vpUpload.addOnPageChangeListener(
            object : OnPageChangeListener {
                /** Handles the corresponding upload page event. */
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) = Unit

                /** Handles the corresponding upload page event. */
                override fun onPageSelected(position: Int) {
                    view.currentSelectedPosition = position
                    if (position >= view.uploadableFiles.size) {
                        view.binding.cvContainerTopCard.visibility = View.GONE
                    } else {
                        view.thumbnailsAdapter!!.notifyDataSetChanged()
                        view.binding.cvContainerTopCard.visibility = View.VISIBLE
                    }
                }

                /** Handles the corresponding upload page event. */
                override fun onPageScrollStateChanged(state: Int) = Unit
            },
        )
    }

    /** Displays the shared upload progress dialog. */
    override fun showProgress(shouldShow: Boolean) = view.showProgress(shouldShow)

    /** Returns the position of an upload page. */
    override fun getIndexInViewFlipper(fragment: UploadBaseFragment?): Int = view.fragments!!.indexOf(fragment)

    /** Advances a step, validates the next image, or submits the upload. */
    override fun onNextButtonClicked(index: Int) {
        if (index < view.fragments!!.size - 1) {
            // Hide the keyboard before navigating to Media License screen
            val isUploadCategoriesFragment =
                view.fragments!!.getOrNull(index)?.let {
                    it is UploadCategoriesFragment
                } ?: false
            if (isUploadCategoriesFragment) {
                val inputMethodManager =
                    activity.getSystemService(
                        android.content.Context.INPUT_METHOD_SERVICE,
                    ) as InputMethodManager
                activity.currentFocus?.let { focusedView ->
                    inputMethodManager.hideSoftInputFromWindow(
                        focusedView.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS,
                    )
                }
            }
            view.binding.vpUpload.setCurrentItem(index + 1, false)
            view.fragments!![index + 1].onBecameVisible()
            (view.binding.rvThumbnails.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(if ((index > 0)) index - 1 else 0, 0)
            if (index < view.fragments!!.size - VALIDATION_STEP_OFFSET) {
                // check image quality if next image exists
                activity.presenter!!.checkImageQuality(index + 1)
            }
        } else {
            activity.presenter!!.handleSubmit()
        }
    }

    /** Returns to the preceding step and restores its thumbnails. */
    override fun onPreviousButtonClicked(index: Int) {
        if (index != 0) {
            view.binding.vpUpload.setCurrentItem(index - 1, true)
            view.fragments!![index - 1].onBecameVisible()
            (view.binding.rvThumbnails.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(if ((index > THUMBNAIL_SCROLL_THRESHOLD)) index - 2 else 0, 0)
            if ((index != 1) && ((index - 1) < view.uploadableFiles.size)) {
                // Shows the top card if it was hidden because of the last image being deleted and
                // now the user has hit previous button to go back to the media details
                view.showHideTopCard(true)
            }
        }
    }

    /** Deletes the selected upload and refreshes thumbnails. */
    override fun onThumbnailDeleted(position: Int) {
        activity.presenter!!.deletePictureAtIndex(position)
        view.thumbnailsAdapter?.notifyDataSetChanged()
    }

    /**
     * Changes current image when one image upload is cancelled, to highlight next image in the top
     * thumbnail.
     * Fixes: [Issue](https://github.com/commons-app/apps-android-commons/issues/5511)
     *
     * @param index Index of image to be removed
     * @param maxSize Max size of the `uploadableFiles`
     */
    fun highlightNextImageOnCancelledImage(
        index: Int,
        maxSize: Int,
    ) {
        if (index < maxSize) {
            view.binding.vpUpload.setCurrentItem(index + 1, false)
            view.binding.vpUpload.setCurrentItem(index, false)
        }
    }

    /** Toggles the thumbnail strip. */
    fun onRlContainerTitleClicked() {
        view.binding.rvThumbnails.visibility =
            if (view.isTitleExpanded) View.GONE else View.VISIBLE
        view.isTitleExpanded = !view.isTitleExpanded
        view.binding.ibToggleTopCard.rotation += HALF_TURN_DEGREES
    }

    companion object {
        private const val VALIDATION_STEP_OFFSET = 4
        private const val THUMBNAIL_SCROLL_THRESHOLD = 3
        private const val HALF_TURN_DEGREES = 180
    }
}
