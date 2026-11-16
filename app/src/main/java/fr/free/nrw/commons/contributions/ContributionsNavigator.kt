package fr.free.nrw.commons.contributions

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.nearby.NearbyNotificationCardView
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.theme.BaseActivity

/** Owns navigation between the contributions list and media details. */
class ContributionsNavigator :
    MediaDetailProvider,
    ContributionsListFragment.Callback,
    ContributionsNavigationActions {
    internal lateinit var fragment: ContributionsFragment

    companion object {
        const val CONTRIBUTION_LIST_FRAGMENT_TAG = "ContributionListFragmentTag"
    }

    /** Restores the initial contributions list and media detail navigation. */
    fun initFragments() {
        if (null == fragment.contributionsListFragment) {
            fragment.contributionsListFragment = ContributionsListFragment()
            val contributionsListBundle = Bundle()
            contributionsListBundle.putString(ProfileActivity.KEY_USERNAME, fragment.userName)
            fragment.contributionsListFragment!!.arguments = contributionsListBundle
        }

        if (fragment.shouldShowMediaDetailsFragment) {
            fragment.renderer.showMediaDetailPagerFragment()
        } else {
            fragment.renderer.showContributionsListFragment()
        }

        fragment.renderer.showFragment(
            fragment.contributionsListFragment!!,
            CONTRIBUTION_LIST_FRAGMENT_TAG,
            fragment.mediaDetailPagerFragment,
        )
    }

    /** Refreshes the media detail pager when contributions change. */
    override fun notifyDataSetChanged() {
        if (fragment.mediaDetailPagerFragment != null) {
            fragment.mediaDetailPagerFragment!!.notifyDataSetChanged()
        }
    }

    /**
     * Notify the viewpager that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        if (fragment.mediaDetailPagerFragment != null) {
            fragment.mediaDetailPagerFragment!!.notifyDataSetChanged()
        }
    }

    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack. Called when user selects
     * a contribution.
     */
    override fun showDetail(
        position: Int,
        isWikipediaButtonDisplayed: Boolean,
    ) {
        if (fragment.mediaDetailPagerFragment == null || !fragment.mediaDetailPagerFragment!!.isVisible) {
            fragment.mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true)
            if (fragment.isUserProfile) {
                (fragment.activity as ProfileActivity).setScroll(false)
            }
            fragment.renderer.showMediaDetailPagerFragment()
        }
        fragment.mediaDetailPagerFragment!!.showImage(position, isWikipediaButtonDisplayed)
    }

    /** Returns the media at the requested contributions list position. */
    override fun getMediaAtPosition(i: Int): Media? = fragment.contributionsListFragment!!.getMediaAtPosition(i)

    /** Returns the number of media items in the contributions list. */
    override fun getTotalMediaCount(): Int = fragment.contributionsListFragment!!.totalMediaCount

    /** Returns the upload state at the requested list position. */
    override fun getContributionStateAt(position: Int): Int =
        fragment.contributionsListFragment!!.getContributionStateAt(position)

    /** Returns to the contributions list from media details. */
    override fun backButtonClicked(): Boolean {
        if (fragment.mediaDetailPagerFragment != null && fragment.mediaDetailPagerFragment!!.isVisible) {
            if (fragment.store!!.getBoolean("displayNearbyCardView", true) && !fragment.isUserProfile) {
                if (fragment.binding!!.cardViewNearby.cardViewVisibilityState
                    == NearbyNotificationCardView.CardViewVisibilityState.READY
                ) {
                    fragment.binding!!.cardViewNearby.visibility = View.VISIBLE
                }
            } else {
                fragment.binding!!.cardViewNearby.visibility = View.GONE
            }
            fragment.renderer.removeFragment(fragment.mediaDetailPagerFragment!!)
            fragment.renderer.showFragment(
                fragment.contributionsListFragment!!,
                CONTRIBUTION_LIST_FRAGMENT_TAG,
                fragment.mediaDetailPagerFragment,
            )
            if (fragment.isUserProfile) {
                // Fragment is associated with ProfileActivity
                // Enable ParentViewPager Scroll
                (fragment.activity as ProfileActivity).setScroll(true)
            } else {
                fragment.campaigns.fetchCampaigns()
            }
            if (fragment.activity is MainActivity) {
                // Fragment is associated with MainActivity
                (fragment.activity as BaseActivity)
                    .supportActionBar
                    ?.setDisplayHomeAsUpEnabled(false)
                (fragment.activity as MainActivity).showTabs()
            }
            return true
        }
        return false
    }

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (fragment.mediaDetailPagerFragment != null && !fragment.contributionsListFragment!!.isVisible) {
            fragment.renderer.removeFragment(fragment.mediaDetailPagerFragment!!)
            fragment.mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true)
            fragment.mediaDetailPagerFragment?.showImage(index)
            fragment.renderer.showMediaDetailPagerFragment()
        }
    }

    /** Scrolls the contributions list to its first item. */
    override fun scrollToTop() {
        if (fragment.contributionsListFragment != null) {
            fragment.contributionsListFragment!!.scrollToTop()
        }
    }
}
