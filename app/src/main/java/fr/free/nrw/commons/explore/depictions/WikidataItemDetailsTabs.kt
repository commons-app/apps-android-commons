package fr.free.nrw.commons.explore.depictions

import android.app.Activity
import androidx.core.os.bundleOf
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.databinding.ActivityWikidataItemDetailsBinding
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsFragment
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsFragment

/** Configures the media, child class and parent class tabs for a Wikidata item. */
internal class WikidataItemDetailsTabs(
    private val activity: Activity,
    private val binding: ActivityWikidataItemDetailsBinding,
    private val viewPagerAdapter: ViewPagerAdapter,
) {
    /**
     * This activity contains 3 tabs and a viewpager. This method is used to set the titles of tab,
     * Set the fragments according to the tab selected in the viewPager.
     */
    fun setTabs(): DepictedImagesFragment {
        val depictionImagesListFragment = DepictedImagesFragment()
        val childDepictionsFragment = ChildDepictionsFragment()
        val parentDepictionsFragment = ParentDepictionsFragment()
        val wikidataItemName = activity.intent.getStringExtra("wikidataItemName")
        val entityId = activity.intent.getStringExtra("entityId")
        if (activity.intent != null && wikidataItemName != null) {
            val arguments =
                bundleOf(
                    "wikidataItemName" to wikidataItemName,
                    "entityId" to entityId,
                )
            depictionImagesListFragment.arguments = arguments
            parentDepictionsFragment.arguments = arguments
            childDepictionsFragment.arguments = arguments
        }

        viewPagerAdapter.setTabs(
            R.string.title_for_media to depictionImagesListFragment,
            R.string.title_for_child_classes to childDepictionsFragment,
            R.string.title_for_parent_classes to parentDepictionsFragment,
        )
        binding.viewPager.offscreenPageLimit = 2
        viewPagerAdapter.notifyDataSetChanged()
        return depictionImagesListFragment
    }

    /**
     * Gets the passed wikidataItemName from the intents and displays it as the page title
     */
    fun setPageTitle() {
        if (activity.intent != null && activity.intent.getStringExtra("wikidataItemName") != null) {
            activity.title = activity.intent.getStringExtra("wikidataItemName")
        }
    }
}
