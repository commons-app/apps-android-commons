package fr.free.nrw.commons.bookmarks

import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment
import fr.free.nrw.commons.category.GridViewAdapter
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.media.clearRemoved
import fr.free.nrw.commons.navtab.NavTab

/**
 * Handles showing and removing the child fragments of the bookmark list screen,
 * and the back button behaviour of that screen.
 *
 * @param root the bookmark list screen whose child fragments are managed
 */
class BookmarkListNavigator(
    private val root: BookmarkListRootFragment,
) {
    /**
     * Shows the given fragment inside the bookmark list screen, hiding the other fragment if any.
     *
     * @param fragment the fragment to show
     * @param otherFragment the fragment to hide, or null if there is none
     */
    fun setFragment(
        fragment: Fragment,
        otherFragment: Fragment?,
    ) {
        val manager = root.childFragmentManager
        if (fragment.isAdded() && otherFragment != null) {
            manager
                .beginTransaction()
                .hide(otherFragment)
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            manager.executePendingTransactions()
        } else if (fragment.isAdded() && otherFragment == null) {
            manager
                .beginTransaction()
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            manager.executePendingTransactions()
        } else if (!fragment.isAdded() && otherFragment != null) {
            manager
                .beginTransaction()
                .hide(otherFragment)
                .add(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            manager.executePendingTransactions()
        } else if (!fragment.isAdded()) {
            manager
                .beginTransaction()
                .replace(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            manager.executePendingTransactions()
        }
    }

    /**
     * Removes the given fragment from the bookmark list screen.
     *
     * @param fragment the fragment to remove
     */
    fun removeFragment(fragment: Fragment) {
        val manager = root.childFragmentManager
        manager
            .beginTransaction()
            .remove(fragment)
            .commit()
        manager.executePendingTransactions()
    }

    /**
     * Handles the back button. If the media details are showing, goes back to the list and
     * removes the pictures that were un-bookmarked there. Otherwise goes to the contributions tab.
     *
     * @return always false, as the event is never fully consumed here
     */
    fun backPressed(): Boolean {
        val details = root.mediaDetails
        if (details == null || !details.isVisible) {
            moveToContributionsFragment()
            return false
        }

        val bookmarkFragment = root.parentFragment as BookmarkFragment
        bookmarkFragment.setupTabLayout()
        val removed = details.removedItems
        removeFragment(details)
        bookmarkFragment.setScroll(true)
        val listFragment = checkNotNull(root.listFragment)
        setFragment(listFragment, details)
        (root.requireActivity() as MainActivity).showTabs()

        if (listFragment is BookmarkPicturesFragment) {
            val adapter = listFragment.getAdapter() as? GridViewAdapter
            removed.forEach { index ->
                adapter?.remove(adapter.getItem(index))
            }
            details.clearRemoved()
        }
        return false
    }

    /**
     * Switches the main screen to the contributions tab and shows the tabs.
     */
    fun moveToContributionsFragment() {
        val activity = root.requireActivity() as MainActivity
        activity.setSelectedItemId(NavTab.CONTRIBUTIONS.code())
        activity.showTabs()
    }
}
