package fr.free.nrw.commons.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentFeaturedRootBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment.MediaDetailProvider
import fr.free.nrw.commons.navtab.NavTab

class ExploreListRootFragment : CommonsDaggerSupportFragment, MediaDetailProvider,
    CategoryImagesCallback {
    private var mediaDetails: MediaDetailPagerFragment? = null
    private var listFragment: CategoriesMediaFragment? = null
    private var binding: FragmentFeaturedRootBinding? = null

    constructor()

    constructor(bundle: Bundle) {
        listFragment = CategoriesMediaFragment().apply {
            arguments = bundleOf(
                "categoryName" to bundle.getString("categoryName")
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)

        binding = FragmentFeaturedRootBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            setFragment(listFragment!!, mediaDetails)
        }
    }

    fun setFragment(fragment: Fragment, otherFragment: Fragment?) {
        if (fragment.isAdded && otherFragment != null) {
            childFragmentManager
                .beginTransaction()
                .hide(otherFragment)
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        } else if (fragment.isAdded && otherFragment == null) {
            childFragmentManager
                .beginTransaction()
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded && otherFragment != null) {
            childFragmentManager
                .beginTransaction()
                .hide(otherFragment)
                .add(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        }
    }

    private fun removeFragment(fragment: Fragment) {
        childFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
        childFragmentManager.executePendingTransactions()
    }

    override fun onMediaClicked(position: Int) {
        if (binding != null) {
            binding!!.exploreContainer.visibility = View.VISIBLE
        }
        if ((parentFragment as ExploreFragment).binding != null) {
            (parentFragment as ExploreFragment).binding.tabLayout.visibility =
                View.GONE
        }
        mediaDetails = MediaDetailPagerFragment.newInstance(false, true)
        (parentFragment as ExploreFragment).setScroll(false)
        setFragment(mediaDetails!!, listFragment)
        mediaDetails!!.showImage(position)
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
    override fun getTotalMediaCount(): Int = listFragment?.totalMediaCount ?: 0

    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (mediaDetails != null && !listFragment!!.isVisible) {
            removeFragment(mediaDetails!!)
            onMediaClicked(index)
        }
    }

    /**
     * This method is called on success of API call for featured images or mobile uploads. The
     * viewpager will notified that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        mediaDetails?.notifyDataSetChanged()
    }

    /**
     * Performs back pressed action on the fragment. Return true if the event was handled by the
     * mediaDetails otherwise returns false.
     *
     * @return
     */
    fun backPressed(): Boolean {
        if (null != mediaDetails && mediaDetails!!.isVisible) {
            if ((parentFragment as ExploreFragment).binding != null) {
                (parentFragment as ExploreFragment).binding.tabLayout.visibility =
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

    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }
}
