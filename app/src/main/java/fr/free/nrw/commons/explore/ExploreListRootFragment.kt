package fr.free.nrw.commons.explore

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment
import fr.free.nrw.commons.navtab.NavTab

class ExploreListRootFragment : ExploreMediaRootFragment {
    private var listFragment: CategoriesMediaFragment? = null
    override val contentFragment: Fragment?
        get() = listFragment

    constructor()

    constructor(bundle: Bundle) {
        listFragment =
            CategoriesMediaFragment().apply {
                arguments =
                    bundleOf(
                        "categoryName" to bundle.getString("categoryName"),
                    )
            }
    }

    /**
     * Keeps opening list media safe when the root view has already been released.
     */
    override fun showMediaContainer() {
        if (binding != null) {
            binding!!.exploreContainer.visibility = View.VISIBLE
        }
    }

    /**
     * Retains the list shell's allowance for an Explore parent without a current tab-strip view.
     */
    override fun setExploreTabsVisibility(visibility: Int) {
        if ((parentFragment as ExploreFragment).binding != null) {
            (parentFragment as ExploreFragment).binding!!.tabLayout.visibility = visibility
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            setFragment(listFragment!!, mediaDetails)
        }
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     *
     * @param i It is the index of which media object is to be returned which is same as current
     * index of viewPager.
     * @return Media Object
     */
    override fun getMediaAtPosition(i: Int): Media? = listFragment?.getMediaAtPosition(i)

    /**
     * This method is called on from getCount of MediaDetailPagerFragment The viewpager will contain
     * same number of media items as that of media elements in adapter.
     *
     * @return Total Media count in the adapter
     */
    override fun getTotalMediaCount(): Int = listFragment?.getTotalMediaCount() ?: 0

    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Performs back pressed action on the fragment. Return true if the event was handled by the
     * mediaDetails otherwise returns false.
     *
     * @return
     */
    fun backPressed(): Boolean {
        if (null != mediaDetails && mediaDetails!!.isVisible) {
            if ((parentFragment as ExploreFragment).binding != null) {
                (parentFragment as ExploreFragment).binding!!.tabLayout.visibility =
                    View.VISIBLE
            }
            removeFragment(mediaDetails!!)
            (parentFragment as ExploreFragment).setScroll(true)
            setFragment(listFragment!!, mediaDetails)
            (activity as MainActivity).showTabs()
            return true
        } else {
            if ((activity as MainActivity?) != null) {
                (activity as MainActivity).setSelectedItemId(NavTab.CONTRIBUTIONS.code())
            }
        }
        if ((activity as MainActivity?) != null) {
            (activity as MainActivity).showTabs()
        }
        return false
    }
}
