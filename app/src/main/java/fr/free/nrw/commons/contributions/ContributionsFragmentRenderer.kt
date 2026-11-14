package fr.free.nrw.commons.contributions

import android.view.View
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.ContributionsFragment.Companion.MEDIA_DETAIL_PAGER_FRAGMENT_TAG
import fr.free.nrw.commons.nearby.NearbyNotificationCardView

/** Owns child fragment transactions and contributions card visibility. */
class ContributionsFragmentRenderer(
    private val fragment: ContributionsFragment,
) {
    /**
     * Replace FrameLayout with ContributionsListFragment, user will see contributions list. Creates
     * new one if null.
     */
    fun showContributionsListFragment() {
        // show nearby card view on contributions list is visible
        if (fragment.binding!!.cardViewNearby != null && !fragment.isUserProfile) {
            if (fragment.store!!.getBoolean("displayNearbyCardView", true)) {
                if (fragment.binding!!.cardViewNearby.cardViewVisibilityState
                    == NearbyNotificationCardView.CardViewVisibilityState.READY
                ) {
                    fragment.binding!!.cardViewNearby.visibility = View.VISIBLE
                }
            } else {
                fragment.binding!!.cardViewNearby.visibility = View.GONE
            }
        }
        showFragment(
            fragment.contributionsListFragment!!,
            ContributionsNavigator.CONTRIBUTION_LIST_FRAGMENT_TAG,
            fragment.mediaDetailPagerFragment,
        )
    }

    /** Displays media details and hides campaign content. */
    fun showMediaDetailPagerFragment() {
        // hide nearby card view on media detail is visible
        setupViewForMediaDetails()
        showFragment(
            fragment.mediaDetailPagerFragment!!,
            MEDIA_DETAIL_PAGER_FRAGMENT_TAG,
            fragment.contributionsListFragment,
        )
    }

    /** Hides the campaign card while displaying media details. */
    private fun setupViewForMediaDetails() {
        if (fragment.binding != null) {
            fragment.binding!!.campaignsView.visibility = View.GONE
        }
    }

    /**
     * Replaces the root frame layout with the given fragment
     *
     * @param fragment
     * @param tag
     * @param otherFragment
     */
    fun showFragment(
        fragment: Fragment,
        tag: String,
        otherFragment: Fragment?,
    ) {
        val transaction = this.fragment.childFragmentManager.beginTransaction()
        if (fragment.isAdded && otherFragment != null) {
            transaction.hide(otherFragment)
            transaction.show(fragment)
            transaction.addToBackStack(tag)
            transaction.commit()
            this.fragment.childFragmentManager.executePendingTransactions()
        } else if (fragment.isAdded && otherFragment == null) {
            transaction.show(fragment)
            transaction.addToBackStack(tag)
            transaction.commit()
            this.fragment.childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded && otherFragment != null) {
            transaction.hide(otherFragment)
            transaction.add(R.id.root_frame, fragment, tag)
            transaction.addToBackStack(tag)
            transaction.commit()
            this.fragment.childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded) {
            transaction.replace(R.id.root_frame, fragment, tag)
            transaction.addToBackStack(tag)
            transaction.commit()
            this.fragment.childFragmentManager.executePendingTransactions()
        }
    }

    /** Removes a child fragment and executes the pending transaction. */
    fun removeFragment(fragment: Fragment) {
        this.fragment.childFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
        this.fragment.childFragmentManager.executePendingTransactions()
    }
}
