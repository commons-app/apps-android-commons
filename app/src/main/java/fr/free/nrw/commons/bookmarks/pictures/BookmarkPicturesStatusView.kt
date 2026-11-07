package fr.free.nrw.commons.bookmarks.pictures

import android.view.View
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentBookmarksPicturesBinding
import fr.free.nrw.commons.utils.ViewUtil.showShortSnackbar
import timber.log.Timber

/**
 * Updates the progress bar and status message of the bookmarked pictures screen
 * for the no internet, error and empty bookmark cases.
 *
 * @param binding the view binding of the bookmarked pictures screen
 */
class BookmarkPicturesStatusView(
    private val binding: FragmentBookmarksPicturesBinding,
) {
    /**
     * Shows the no internet message, or a snackbar if pictures are already displayed.
     *
     * @param isGridEmpty true if the grid has no pictures yet
     */
    fun showNoInternet(isGridEmpty: Boolean) {
        binding.loadingImagesProgressBar.visibility = View.GONE
        if (isGridEmpty) {
            binding.statusMessage.visibility = View.VISIBLE
            binding.statusMessage.text = binding.root.context.getString(R.string.no_internet)
        } else {
            showShortSnackbar(binding.parentLayout, R.string.no_internet)
        }
    }

    /**
     * Logs an error that happened while loading pictures and shows the error state.
     *
     * @param throwable the error that happened
     * @param isGridEmpty true if the grid has no pictures yet
     */
    @Suppress("TooGenericExceptionCaught")
    fun showError(
        throwable: Throwable,
        isGridEmpty: Boolean,
    ) {
        Timber.e(throwable, "Error occurred while loading images inside a category")
        try {
            showShortSnackbar(binding.root, R.string.error_loading_images)
            showErrorView(isGridEmpty)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Hides the progress bar and shows the "no images found" message if the grid is empty.
     *
     * @param isGridEmpty true if the grid has no pictures yet
     */
    fun showErrorView(isGridEmpty: Boolean) {
        binding.loadingImagesProgressBar.visibility = View.GONE
        if (isGridEmpty) {
            binding.statusMessage.visibility = View.VISIBLE
            binding.statusMessage.text = binding.root.context.getString(R.string.no_images_found)
        } else {
            binding.statusMessage.visibility = View.GONE
        }
    }

    /**
     * Hides the progress bar and shows the "no bookmarks" message if the grid is empty.
     *
     * @param isGridEmpty true if the grid has no pictures yet
     */
    fun showEmptyBookmarks(isGridEmpty: Boolean) {
        binding.loadingImagesProgressBar.visibility = View.GONE
        if (isGridEmpty) {
            binding.statusMessage.visibility = View.VISIBLE
            binding.statusMessage.text = binding.root.context.getString(R.string.bookmark_empty)
        } else {
            binding.statusMessage.visibility = View.GONE
        }
    }
}
