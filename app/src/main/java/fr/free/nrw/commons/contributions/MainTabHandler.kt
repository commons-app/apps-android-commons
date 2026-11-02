package fr.free.nrw.commons.contributions

import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.navtab.NavTabLayout
import fr.free.nrw.commons.navtab.NavTabLoggedOut

/**
 * Manages bottom navigation tabs, item selection, visibility, and title updates for [MainActivity].
 */
class MainTabHandler(
    private val activity: MainActivity,
    private val tabLayout: NavTabLayout,
) {
    /**
     * Listener registered on bottom navigation to handle tab selections.
     */
    var navListener: BottomNavigationView.OnNavigationItemSelectedListener? = null
        private set

    /**
     * Configures the bottom navigation listener for logged-in users.
     */
    fun setUpPager(onTabSelected: (Fragment?, Boolean) -> Boolean) {
        tabLayout.setOnNavigationItemSelectedListener(
            BottomNavigationView
                .OnNavigationItemSelectedListener { item: MenuItem ->
                    if (item.title != activity.getString(R.string.more)) {
                        activity.title = item.title
                    }
                    activity.applicationKvStore?.putBoolean(
                        "last_opened_nearby",
                        item.title == activity.getString(R.string.nearby_fragment),
                    )
                    val fragment = NavTab.of(item.order).newInstance()
                    onTabSelected(fragment, true)
                }.also { navListener = it },
        )
    }

    /**
     * Configures the bottom navigation listener for logged-out / anonymous users.
     */
    fun setUpLoggedOutPager(onTabSelected: (Fragment?, Boolean) -> Boolean) {
        tabLayout.setOnNavigationItemSelectedListener { item: MenuItem ->
            if (item.title != activity.getString(R.string.more)) {
                activity.title = item.title
            }
            val fragment = NavTabLoggedOut.of(item.order).newInstance()
            onTabSelected(fragment, true)
        }
    }

    /**
     * Makes the bottom navigation tabs visible.
     */
    fun showTabs() {
        tabLayout.visibility = View.VISIBLE
    }

    /**
     * Hides the bottom navigation tabs.
     */
    fun hideTabs() {
        tabLayout.visibility = View.GONE
    }

    /**
     * Selects the tab item with the specified ID in the bottom navigation bar.
     */
    fun setSelectedItemId(id: Int) {
        tabLayout.selectedItemId = id
    }

    /**
     * Selects and navigates to the Nearby tab.
     */
    fun showNearby() {
        setSelectedItemId(NavTab.NEARBY.code())
    }

    /**
     * Updates activity title with upload count when Contributions tab is active.
     */
    fun setNumOfUploads(
        uploadCount: Int,
        activeFragment: ActiveFragment?,
    ) {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            val subtitle =
                if (uploadCount != 0) {
                    activity.resources.getQuantityString(
                        R.plurals.contributions_subtitle,
                        uploadCount,
                        uploadCount,
                    )
                } else {
                    activity.getString(R.string.contributions_subtitle_zero)
                }
            activity.title = "${activity.resources.getString(R.string.contributions_fragment)} $subtitle"
        }
    }
}
