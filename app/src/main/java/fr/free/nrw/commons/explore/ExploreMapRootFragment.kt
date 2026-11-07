package fr.free.nrw.commons.explore

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.explore.map.ExploreMapFragment
import fr.free.nrw.commons.navtab.NavTab

class ExploreMapRootFragment : ExploreMediaRootFragment {
    private var mapFragment: ExploreMapFragment? = null
    override val contentFragment: Fragment?
        get() = mapFragment

    constructor()

    constructor(bundle: Bundle) {
        // get fragment arguments
        val title = bundle.getString("categoryName")

        val zoom = if (bundle.containsKey("prev_zoom")) bundle.getDouble("prev_zoom") else 0.0
        val latitude = if (bundle.containsKey("prev_latitude")) bundle.getDouble("prev_latitude") else 0.0
        val longitude = if (bundle.containsKey("prev_longitude")) bundle.getDouble("prev_longitude") else 0.0

        mapFragment = ExploreMapFragment()
        val featuredArguments =
            bundleOf(
                "categoryName" to title,
            )

        // if we came from 'Show in Explore' in Nearby, pass on zoom and center
        if (bundle.containsKey("prev_zoom") ||
            bundle.containsKey("prev_latitude") ||
            bundle.containsKey("prev_longitude")
        ) {
            featuredArguments.putDouble("prev_zoom", zoom)
            featuredArguments.putDouble("prev_latitude", latitude)
            featuredArguments.putDouble("prev_longitude", longitude)
        }
        mapFragment!!.arguments = featuredArguments
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            setFragment(mapFragment!!, mediaDetails)
        }
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
        fun newInstance(): ExploreMapRootFragment =
            ExploreMapRootFragment().apply {
                retainInstance = true
            }
    }
}
