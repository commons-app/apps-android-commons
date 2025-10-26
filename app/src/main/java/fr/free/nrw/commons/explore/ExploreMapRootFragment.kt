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
import fr.free.nrw.commons.explore.map.ExploreMapFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.navtab.NavTab

class ExploreMapRootFragment : CommonsDaggerSupportFragment, MediaDetailProvider,
    CategoryImagesCallback {
    private var mediaDetails: MediaDetailPagerFragment? = null
    private var mapFragment: ExploreMapFragment? = null
    private var binding: FragmentFeaturedRootBinding? = null

    constructor()

    constructor(bundle: Bundle) {
        // get fragment arguments
        val title = bundle.getString("categoryName")

        val zoom = if (bundle.containsKey("prev_zoom")) bundle.getDouble("prev_zoom") else 0.0
        val latitude = if (bundle.containsKey("prev_latitude")) bundle.getDouble("prev_latitude") else 0.0
        val longitude = if (bundle.containsKey("prev_longitude")) bundle.getDouble("prev_longitude") else 0.0

        mapFragment = ExploreMapFragment()
        val featuredArguments = bundleOf(
            "categoryName" to title
        )

        // if we came from 'Show in Explore' in Nearby, pass on zoom and center
        if (bundle.containsKey("prev_zoom") || bundle.containsKey("prev_latitude") || bundle.containsKey("prev_longitude")) {
            featuredArguments.putDouble("prev_zoom", zoom)
            featuredArguments.putDouble("prev_latitude", latitude)
            featuredArguments.putDouble("prev_longitude", longitude)
        }
        mapFragment!!.arguments = featuredArguments
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
            setFragment(mapFragment!!, mediaDetails)
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
        binding!!.exploreContainer.visibility = View.VISIBLE
        (parentFragment as ExploreFragment).binding!!.tabLayout.visibility = View.GONE
        mediaDetails = MediaDetailPagerFragment.newInstance(false, true)
        (parentFragment as ExploreFragment).setScroll(false)
        setFragment(mediaDetails!!, mapFragment)
        mediaDetails!!.showImage(position)
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     *
     * @param i It is the index of which media object is to be returned which is same as current
     * index of viewPager.
     * @return Media Object
     */
    override fun getMediaAtPosition(i: Int): Media? = mapFragment?.mediaList?.get(i)

    /**
     * This method is called on from getCount of MediaDetailPagerFragment The viewpager will contain
     * same number of media items as that of media elements in adapter.
     *
     * @return Total Media count in the adapter
     */
    override fun getTotalMediaCount(): Int = mapFragment?.mediaList?.size ?: 0

    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (mediaDetails != null && !mapFragment!!.isVisible) {
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
            (parentFragment as ExploreFragment).binding!!.tabLayout.visibility = View.VISIBLE
            removeFragment(mediaDetails!!)
            (parentFragment as ExploreFragment).setScroll(true)
            setFragment(mapFragment!!, mediaDetails)
            (activity as MainActivity).showTabs()
            return true
        }
        if (mapFragment != null && mapFragment!!.isVisible) {
            if (mapFragment!!.backButtonClicked()) {
                // Explore map fragment handled the event no further action required.
                return true
            } else {
                (activity as MainActivity).showTabs()
                return false
            }
        } else {
            (activity as MainActivity).setSelectedItemId(NavTab.CONTRIBUTIONS.code())
        }
        (activity as MainActivity).showTabs()
        return false
    }

    fun loadNearbyMapFromExplore() = mapFragment?.loadNearbyMapFromExplore()

    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }

    fun requestLocationIfNeeded() {
        mapFragment?.requestLocationIfNeeded()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            requestLocationIfNeeded()
        }
    }

    companion object {
        fun newInstance(): ExploreMapRootFragment = ExploreMapRootFragment().apply {
            retainInstance = true
        }
    }
}