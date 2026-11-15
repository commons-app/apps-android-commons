package fr.free.nrw.commons.contributions

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.BookmarkFragment
import fr.free.nrw.commons.contributions.ContributionsFragment.Companion.newInstance
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.explore.ExploreFragment
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback

/**
 * Handles fragment transactions, back-stack navigation, state restoration,
 * and cross-tab map loading for [MainActivity].
 */
class MainNavigationHelper(
    private val activity: MainActivity,
    private val tabHandler: MainTabHandler,
) {
    var contributionsFragment: ContributionsFragment? = null
    var nearbyParentFragment: NearbyParentFragment? = null
    var exploreFragment: ExploreFragment? = null
    var bookmarkFragment: BookmarkFragment? = null
    var activeFragment: ActiveFragment? = null

    /**
     * Loads the fragment into the main container, handling tab deduplication,
     * scroll-to-top on reselection, and bottom sheet presentation when fragment is null.
     */
    fun loadFragment(
        fragment: Fragment?,
        showBottom: Boolean,
        args: Bundle? = null,
    ): Boolean {
        if (fragment != null && args != null) {
            fragment.arguments = args
        }

        freeUpFragments()

        if (fragment == null) {
            if (showBottom) {
                val isLoggedOut = activity.applicationKvStore?.getBoolean("login_skipped") == true
                val bottomSheet =
                    if (isLoggedOut) {
                        MoreBottomSheetLoggedOutFragment()
                    } else {
                        MoreBottomSheetFragment()
                    }
                val tag = if (isLoggedOut) "MoreBottomSheetLoggedOut" else "MoreBottomSheet"
                bottomSheet.show(activity.supportFragmentManager, tag)
            }
            return false
        }

        if (!isSameTab(fragment)) {
            updateActiveFragment(fragment)
            activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
        return true
    }

    /**
     * Checks if the fragment represents the currently active tab, scrolling to top if so.
     */
    private fun isSameTab(fragment: Fragment): Boolean {
        if (fragment is ContributionsFragment && activeFragment == ActiveFragment.CONTRIBUTIONS) {
            contributionsFragment?.scrollToTop()
            return true
        }
        return (fragment is NearbyParentFragment && activeFragment == ActiveFragment.NEARBY) ||
            (fragment is ExploreFragment && activeFragment == ActiveFragment.EXPLORE) ||
            (fragment is BookmarkFragment && activeFragment == ActiveFragment.BOOKMARK)
    }

    /**
     * Updates active fragment references and current [ActiveFragment] state.
     */
    private fun updateActiveFragment(fragment: Fragment) {
        when (fragment) {
            is ContributionsFragment -> {
                contributionsFragment = fragment
                activeFragment = ActiveFragment.CONTRIBUTIONS
            }
            is NearbyParentFragment -> {
                nearbyParentFragment = fragment
                activeFragment = ActiveFragment.NEARBY
            }
            is ExploreFragment -> {
                exploreFragment = fragment
                activeFragment = ActiveFragment.EXPLORE
            }
            is BookmarkFragment -> {
                bookmarkFragment = fragment
                activeFragment = ActiveFragment.BOOKMARK
            }
        }
    }

    /**
     * Clears fragment references to prevent memory leaks when switching tabs.
     */
    fun freeUpFragments() {
        nearbyParentFragment = null
        exploreFragment = null
        bookmarkFragment = null
    }

    /**
     * Restores the active tab and fragment from saved instance state.
     */
    fun restoreActiveFragment(fragmentName: String) {
        when (fragmentName) {
            ActiveFragment.CONTRIBUTIONS.name -> {
                activity.title = activity.getString(R.string.contributions_fragment)
                loadFragment(newInstance(), false)
            }
            ActiveFragment.NEARBY.name -> {
                activity.title = activity.getString(R.string.nearby_fragment)
                loadFragment(NearbyParentFragment.newInstance(), false)
            }
            ActiveFragment.EXPLORE.name -> {
                activity.title = activity.getString(R.string.navigation_item_explore)
                loadFragment(ExploreFragment.newInstance(), false)
            }
            ActiveFragment.BOOKMARK.name -> {
                activity.title = activity.getString(R.string.bookmarks)
                loadFragment(BookmarkFragment.newInstance(), false)
            }
        }
    }

    /**
     * Registers and returns an [OnBackPressedCallback] for custom tab back-stack routing.
     */
    fun setupBackNavigation(): OnBackPressedCallback {
        val callback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val dispatchSystemBack = {
                        isEnabled = false
                        activity.onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                    when (activeFragment) {
                        ActiveFragment.CONTRIBUTIONS -> {
                            if (contributionsFragment?.backButtonClicked() != true) {
                                dispatchSystemBack()
                            }
                        }
                        ActiveFragment.NEARBY -> {
                            if (nearbyParentFragment?.backButtonClicked() != true) {
                                nearbyParentFragment?.let {
                                    activity.supportFragmentManager
                                        .beginTransaction()
                                        .remove(it)
                                        .commit()
                                }
                                tabHandler.setSelectedItemId(NavTab.CONTRIBUTIONS.code())
                            }
                        }
                        ActiveFragment.EXPLORE -> {
                            if (exploreFragment?.onBackPressed() != true) {
                                if (activity.applicationKvStore?.getBoolean("login_skipped") == true) {
                                    dispatchSystemBack()
                                } else {
                                    tabHandler.setSelectedItemId(NavTab.CONTRIBUTIONS.code())
                                }
                            }
                        }
                        ActiveFragment.BOOKMARK -> {
                            bookmarkFragment?.onBackPressed()
                            tabHandler.setSelectedItemId(NavTab.CONTRIBUTIONS.code())
                        }
                        else -> dispatchSystemBack()
                    }
                }
            }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        return callback
    }

    /**
     * Handles Up navigation from the toolbar.
     */
    fun onSupportNavigateUp(): Boolean {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            if (contributionsFragment?.backButtonClicked() != true) {
                return false
            }
        } else {
            activity.onBackPressedDispatcher.onBackPressed()
            tabHandler.showTabs()
        }
        return true
    }

    /**
     * Centers the Nearby map on the specified place once the fragment is ready.
     */
    fun centerMapToPlace(place: Place?) {
        tabHandler.setSelectedItemId(NavTab.NEARBY.code())
        nearbyParentFragment?.setNearbyParentFragmentInstanceReadyCallback(
            object : NearbyParentFragmentInstanceReadyCallback {
                override fun onReady() {
                    nearbyParentFragment?.centerMapToPlace(place)
                }
            },
        )
    }

    /**
     * Launches the Explore map with coordinates and zoom level passed from Nearby.
     */
    fun loadExploreMapFromNearby(
        zoom: Double,
        latitude: Double,
        longitude: Double,
    ) {
        val bundle =
            Bundle().apply {
                putDouble("prev_zoom", zoom)
                putDouble("prev_latitude", latitude)
                putDouble("prev_longitude", longitude)
            }
        loadFragment(ExploreFragment.newInstance(), false, bundle)
        tabHandler.setSelectedItemId(NavTab.EXPLORE.code())
    }

    /**
     * Launches the Nearby map with coordinates and zoom level passed from Explore.
     */
    fun loadNearbyMapFromExplore(
        zoom: Double,
        latitude: Double,
        longitude: Double,
    ) {
        val bundle =
            Bundle().apply {
                putDouble("prev_zoom", zoom)
                putDouble("prev_latitude", latitude)
                putDouble("prev_longitude", longitude)
            }
        loadFragment(NearbyParentFragment.newInstance(), false, bundle)
        tabHandler.setSelectedItemId(NavTab.NEARBY.code())
    }
}
