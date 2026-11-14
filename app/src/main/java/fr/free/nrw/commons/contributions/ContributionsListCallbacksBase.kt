package fr.free.nrw.commons.contributions

import android.view.View
import fr.free.nrw.commons.media.DeletedFileActivity

abstract class ContributionsListCallbacksBase :
    BaseContributionsListFragment(),
    ContributionsListAdapter.Callback {
    abstract val wikipediaHelper: ContributionsListWikipediaHelper

    /**
     * Shows welcome message if user has no contributions yet i.e. new user.
     */
    override fun showWelcomeTip(numberOfUploads: Boolean) {
        binding!!.noContributionsYet.visibility =
            if (numberOfUploads) View.VISIBLE else View.GONE
    }

    /**
     * Responsible to set progress bar invisible and visible
     *
     * @param shouldShow True when contributions list should be hidden.
     */
    override fun showProgress(shouldShow: Boolean) {
        binding!!.loadingContributionsProgressBar.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun showNoContributionsUI(shouldShow: Boolean) {
        binding!!.noContributionsYet.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun openMediaDetail(
        contribution: Int,
        isWikipediaPageExists: Boolean,
    ) {
        val item = adapter?.getContributionForPosition(contribution)
        val filename = item?.media?.filename
        if (item?.state == Contribution.STATE_DELETED && !filename.isNullOrEmpty()) {
            DeletedFileActivity.start(requireContext(), filename)
            return
        }
        if (null != callback) { // Just being safe, ideally they won't be called when detached
            callback!!.showDetail(contribution, isWikipediaPageExists)
        }
    }

    /**
     * Handle callback for wikipedia icon clicked
     *
     * @param contribution
     */
    override fun addImageToWikipedia(contribution: Contribution?) {
        wikipediaHelper.addImageToWikipedia(contribution)
    }
}
