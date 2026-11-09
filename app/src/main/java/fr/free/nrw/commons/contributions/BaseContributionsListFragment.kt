package fr.free.nrw.commons.contributions

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.databinding.FragmentContributionsListBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment

/**
 * Base fragment class providing MVP view callbacks, lifecycle state persistence,
 * and media query operations for contributions list presentation.
 */
abstract class BaseContributionsListFragment :
    CommonsDaggerSupportFragment(),
    ContributionsListContract.View {
    @VisibleForTesting
    var rvContributionsList: RecyclerView? = null

    @VisibleForTesting
    var adapter: ContributionsListAdapter? = null

    @VisibleForTesting
    var callback: ContributionsListFragment.Callback? = null

    var contributionsSize = 0

    var binding: FragmentContributionsListBinding? = null

    val totalMediaCount: Int
        get() = contributionsSize

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is ContributionsFragment) {
            callback = (parentFragment as ContributionsFragment)
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null // To avoid possible memory leak
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val layoutManager = rvContributionsList?.layoutManager as GridLayoutManager?
        outState.putParcelable(RV_STATE, layoutManager?.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (null != savedInstanceState) {
            val savedRecyclerLayoutState =
                BundleCompat.getParcelable(savedInstanceState, RV_STATE, Parcelable::class.java)
            rvContributionsList?.layoutManager?.onRestoreInstanceState(savedRecyclerLayoutState)
        }
    }

    /**
     * Shows welcome message if user has no contributions yet i.e. new user.
     */
    override fun showWelcomeTip(numberOfUploads: Boolean) {
        binding?.noContributionsYet?.visibility =
            if (numberOfUploads) View.VISIBLE else View.GONE
    }

    /**
     * Responsible to set progress bar invisible and visible
     *
     * @param shouldShow True when contributions list should be hidden.
     */
    override fun showProgress(shouldShow: Boolean) {
        binding?.loadingContributionsProgressBar?.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun showNoContributionsUI(shouldShow: Boolean) {
        binding?.noContributionsYet?.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    fun scrollToTop() {
        rvContributionsList?.smoothScrollToPosition(0)
    }

    fun getMediaAtPosition(i: Int): Media? {
        if (adapter?.getContributionForPosition(i) != null) {
            return adapter?.getContributionForPosition(i)?.media
        }
        return null
    }

    fun getContributionStateAt(position: Int): Int = adapter?.getContributionForPosition(position)?.state ?: 0

    companion object {
        const val RV_STATE = "rv_scroll_state"
    }
}
