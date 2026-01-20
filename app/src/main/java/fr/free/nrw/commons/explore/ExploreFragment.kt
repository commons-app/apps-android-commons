package fr.free.nrw.commons.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentExploreBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags
import javax.inject.Inject
import javax.inject.Named

class ExploreFragment : CommonsDaggerSupportFragment() {

    @JvmField
    @Inject
    @Named("default_preferences")
    var applicationKvStore: JsonKvStore? = null

    private var featuredRootFragment: ExploreListRootFragment? = null
    private var mobileRootFragment: ExploreListRootFragment? = null
    private var mapRootFragment: ExploreMapRootFragment? = null
    private var prevZoom = 0.0
    private var prevLatitude = 0.0
    private var prevLongitude = 0.0
    private var viewPagerAdapter: ViewPagerAdapter? = null
    var binding: FragmentExploreBinding? = null

    fun setScroll(canScroll: Boolean) {
        if (binding != null) {
            binding!!.viewPager.canScroll = canScroll
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        loadNearbyMapData()
        binding = FragmentExploreBinding.inflate(inflater, container, false)

        viewPagerAdapter = ViewPagerAdapter(
            requireContext(), childFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )

        binding!!.viewPager.adapter = viewPagerAdapter
        binding!!.viewPager.id = R.id.viewPager
        binding!!.tabLayout.setupWithViewPager(binding!!.viewPager)
        binding!!.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit
            override fun onPageScrollStateChanged(state: Int) = Unit
            override fun onPageSelected(position: Int) {
                binding!!.viewPager.canScroll = position != 2
                // based on which tab we are now recreate the options menu
                activity?.invalidateOptionsMenu()

                if (position == 2) {
                    mapRootFragment?.requestLocationIfNeeded()
                }
            }
        })
        setTabs()
        setHasOptionsMenu(true)

        // if we came from 'Show in Explore' in Nearby, jump to Map tab
        if (isCameFromNearbyMap) {
            binding!!.viewPager.currentItem = 2
        }
        return binding!!.root
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    fun setTabs() {
        val featuredArguments = Bundle()
        featuredArguments.putString("categoryName", FEATURED_IMAGES_CATEGORY)

        val mobileArguments = Bundle()
        mobileArguments.putString("categoryName", MOBILE_UPLOADS_CATEGORY)

        val mapArguments = Bundle()
        mapArguments.putString("categoryName", EXPLORE_MAP)

        // if we came from 'Show in Explore' in Nearby, pass on zoom and center to Explore map root
        if (isCameFromNearbyMap) {
            mapArguments.putDouble("prev_zoom", prevZoom)
            mapArguments.putDouble("prev_latitude", prevLatitude)
            mapArguments.putDouble("prev_longitude", prevLongitude)
        }

        featuredRootFragment = ExploreListRootFragment(featuredArguments)
        mobileRootFragment = ExploreListRootFragment(mobileArguments)
        mapRootFragment = ExploreMapRootFragment(mapArguments)

        (activity as MainActivity).showTabs()
        (activity as BaseActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        viewPagerAdapter!!.setTabs(
            R.string.explore_tab_title_featured to featuredRootFragment!!,
            R.string.explore_tab_title_mobile to mobileRootFragment!!,
            R.string.explore_tab_title_map to mapRootFragment!!
        )
        viewPagerAdapter!!.notifyDataSetChanged()
    }

    /**
     * Fetch Nearby map camera data from fragment arguments if any.
     */
    private fun loadNearbyMapData() {
        // get fragment arguments
        if (arguments != null) {
            with (requireArguments()) {
                if (containsKey("prev_zoom")) {
                    prevZoom = getDouble("prev_zoom")
                }
                if (containsKey("prev_latitude")) {
                    prevLatitude = getDouble("prev_latitude")
                }
                if (containsKey("prev_longitude")) {
                    prevLongitude = getDouble("prev_longitude")
                }
            }
        }
    }

    /**
     * Checks if fragment arguments contain data from Nearby map. if present, then the user
     * navigated from Nearby using 'Show in Explore'.
     *
     * @return true if user navigated from Nearby map
     */
    private val isCameFromNearbyMap: Boolean
        get() = (arguments?.containsKey("prev_zoom") == true
                && arguments?.containsKey("prev_latitude") == true
                && arguments?.containsKey("prev_longitude") == true)

    fun onBackPressed(): Boolean {
        if (binding!!.tabLayout.selectedTabPosition == 0) {
            if (featuredRootFragment!!.backPressed()) {
                (activity as BaseActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                return true
            }
        } else if (binding!!.tabLayout.selectedTabPosition == 1) { //Mobile root fragment
            if (mobileRootFragment!!.backPressed()) {
                (activity as BaseActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                return true
            }
        } else { //explore map fragment
            if (mapRootFragment!!.backPressed()) {
                (activity as BaseActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                return true
            }
        }
        return false
    }

    /**
     * This method inflates the menu in the toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // if logged in 'Show in Nearby' menu item is visible
        if (applicationKvStore!!.getBoolean("login_skipped") == false) {
            inflater.inflate(R.menu.explore_fragment_menu, menu)

            val others = menu.findItem(R.id.list_item_show_in_nearby)

            if (binding!!.viewPager.currentItem == 2) {
                others.setVisible(true)
            }
        } else {
            inflater.inflate(R.menu.menu_search, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * This method handles the logic on ItemSelect in toolbar menu Currently only 1 choice is
     * available to open search page of the app
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.action_search -> {
                startActivityWithFlags(requireActivity(), SearchActivity::class.java)
                return true
            }

            R.id.list_item_show_in_nearby -> {
                mapRootFragment!!.loadNearbyMapFromExplore()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        private const val FEATURED_IMAGES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons"
        private const val MOBILE_UPLOADS_CATEGORY = "Uploaded_with_Mobile/Android"
        private const val EXPLORE_MAP = "Map"

        fun newInstance(): ExploreFragment = ExploreFragment().apply {
            retainInstance = true
        }
    }
}