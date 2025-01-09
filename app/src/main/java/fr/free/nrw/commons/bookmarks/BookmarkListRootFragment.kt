package fr.free.nrw.commons.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesFragment
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsFragment
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.category.GridViewAdapter
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentFeaturedRootBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.navtab.NavTab
import timber.log.Timber


class BookmarkListRootFragment : CommonsDaggerSupportFragment,
    FragmentManager.OnBackStackChangedListener,
    MediaDetailPagerFragment.MediaDetailProvider,
    AdapterView.OnItemClickListener,
    CategoryImagesCallback {

    private var mediaDetails: MediaDetailPagerFragment? = null
    private var bookmarkLocationsFragment: BookmarkLocationsFragment? = null
    var listFragment: Fragment? = null
    private var bookmarksPagerAdapter: BookmarksPagerAdapter? = null

    private var binding: FragmentFeaturedRootBinding? = null

    constructor() : super() {
        // Empty constructor necessary otherwise crashes on recreate
    }

    constructor(bundle: Bundle, bookmarksPagerAdapter: BookmarksPagerAdapter) : this() {
        val title = bundle.getString("categoryName")
        val order = bundle.getInt("order")
        val orderItem = bundle.getInt("orderItem")
        listFragment = when (order) {
            0 -> BookmarkPicturesFragment()
            1 -> BookmarkLocationsFragment()
            3 -> BookmarkCategoriesFragment()
            else -> null
        }

        if(orderItem == 2) {
            listFragment = BookmarkItemsFragment()
        }

        val featuredArguments = Bundle().apply {
            putString("categoryName", title)
        }
        listFragment?.arguments = featuredArguments
        this.bookmarksPagerAdapter = bookmarksPagerAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentFeaturedRootBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            setFragment(listFragment, mediaDetails)
        }
    }

    fun setFragment(fragment: Fragment?, otherFragment: Fragment?) {
        val transaction = childFragmentManager.beginTransaction()
        when {
            fragment?.isAdded == true && otherFragment != null -> {
                transaction.hide(otherFragment).show(fragment)
            }
            fragment?.isAdded == true && otherFragment == null -> {
                transaction.show(fragment)
            }
            fragment?.isAdded == false && otherFragment != null -> {
                transaction.hide(otherFragment).add(R.id.explore_container, fragment)
            }
            fragment?.isAdded == false -> {
                transaction.replace(R.id.explore_container, fragment)
            }
        }
        transaction.addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG").commit()
        childFragmentManager.executePendingTransactions()
    }

    fun removeFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction().remove(fragment).commit()
        childFragmentManager.executePendingTransactions()
    }

    override fun onMediaClicked(position: Int) {
        Timber.tag("deneme8").d("on media clicked")
        // container.setVisibility(View.VISIBLE);
        // ((BookmarkFragment)getParentFragment()).tabLayout.setVisibility(View.GONE);
        // mediaDetails = new MediaDetailPagerFragment(false, true, position);
        // setFragment(mediaDetails, bookmarkPicturesFragment);
    }

    override fun getMediaAtPosition(i: Int): Media? {
        return bookmarksPagerAdapter?.getMediaAdapter()?.let {
            it.getItem(i) as? Media
        }
    }

    override fun getTotalMediaCount(): Int {
        return bookmarksPagerAdapter?.getMediaAdapter()?.count ?: 0
    }

    override fun getContributionStateAt(position: Int): Int? {
        return null
    }

    override fun refreshNominatedMedia(index: Int) {
        if (mediaDetails != null && listFragment?.isVisible == false) {
            removeFragment(mediaDetails!!)
            mediaDetails = MediaDetailPagerFragment.newInstance(false, true)
            (parentFragment as? BookmarkFragment)?.setScroll(false)
            setFragment(mediaDetails, listFragment)
            mediaDetails?.showImage(index)
        }
    }

    override fun viewPagerNotifyDataSetChanged() {
        mediaDetails?.notifyDataSetChanged()
    }

    fun backPressed(): Boolean {
        if (mediaDetails != null) {
            if (mediaDetails!!.isVisible) {
                (parentFragment as? BookmarkFragment)?.setupTabLayout()
                val removed = mediaDetails!!.removedItems
                removeFragment(mediaDetails!!)
                (parentFragment as? BookmarkFragment)?.setScroll(true)
                setFragment(listFragment, mediaDetails)
                (activity as? MainActivity)?.showTabs()
                if (listFragment is BookmarkPicturesFragment) {
                    val adapter = (listFragment as BookmarkPicturesFragment).getAdapter()
                            as GridViewAdapter
                    for (i in removed) {
                        adapter.remove(adapter.getItem(i))
                    }
                    mediaDetails!!.clearRemoved()
                }
            } else {
                moveToContributionsFragment()
            }
        } else {
            moveToContributionsFragment()
        }
        return false
    }

    private fun moveToContributionsFragment() {
        (activity as? MainActivity)?.apply {
            setSelectedItemId(NavTab.CONTRIBUTIONS.code())
            showTabs()
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Timber.tag("deneme8").d("on media clicked")
        binding?.exploreContainer?.visibility = View.VISIBLE
        (parentFragment as? BookmarkFragment)?.binding?.tabLayout?.visibility = View.GONE
        mediaDetails = MediaDetailPagerFragment.newInstance(false, true)
        (parentFragment as? BookmarkFragment)?.setScroll(false)
        setFragment(mediaDetails, listFragment)
        mediaDetails?.showImage(position)
    }

    override fun onBackStackChanged() {}

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
