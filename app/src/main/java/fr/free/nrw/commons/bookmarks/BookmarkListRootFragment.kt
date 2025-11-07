package fr.free.nrw.commons.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
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
import fr.free.nrw.commons.media.MediaDetailPagerFragment.Companion.newInstance
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.navtab.NavTab
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class BookmarkListRootFragment : CommonsDaggerSupportFragment,
    FragmentManager.OnBackStackChangedListener, MediaDetailProvider, OnItemClickListener,
    CategoryImagesCallback {
    private var mediaDetails: MediaDetailPagerFragment? = null
    private val bookmarkLocationsFragment: BookmarkLocationsFragment? = null
    var listFragment: Fragment? = null
    private var bookmarksPagerAdapter: BookmarksPagerAdapter? = null

    var binding: FragmentFeaturedRootBinding? = null

    constructor()

    constructor(bundle: Bundle, bookmarksPagerAdapter: BookmarksPagerAdapter) {
        val title = bundle.getString("categoryName")
        val order = bundle.getInt("order")
        val orderItem = bundle.getInt("orderItem")

        when (order) {
            0 -> listFragment = BookmarkPicturesFragment()
            1 -> listFragment = BookmarkLocationsFragment()
            3 -> listFragment = BookmarkCategoriesFragment()
        }
        if (orderItem == 2) {
            listFragment = BookmarkItemsFragment()
        }

        val featuredArguments = Bundle()
        featuredArguments.putString("categoryName", title)
        listFragment!!.setArguments(featuredArguments)
        this.bookmarksPagerAdapter = bookmarksPagerAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        binding = FragmentFeaturedRootBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            setFragment(listFragment!!, mediaDetails)
        }
    }

    fun setFragment(fragment: Fragment, otherFragment: Fragment?) {
        if (fragment.isAdded() && otherFragment != null) {
            getChildFragmentManager()
                .beginTransaction()
                .hide(otherFragment)
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            getChildFragmentManager().executePendingTransactions()
        } else if (fragment.isAdded() && otherFragment == null) {
            getChildFragmentManager()
                .beginTransaction()
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            getChildFragmentManager().executePendingTransactions()
        } else if (!fragment.isAdded() && otherFragment != null) {
            getChildFragmentManager()
                .beginTransaction()
                .hide(otherFragment)
                .add(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            getChildFragmentManager().executePendingTransactions()
        } else if (!fragment.isAdded()) {
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            getChildFragmentManager().executePendingTransactions()
        }
    }

    fun removeFragment(fragment: Fragment) {
        getChildFragmentManager()
            .beginTransaction()
            .remove(fragment)
            .commit()
        getChildFragmentManager().executePendingTransactions()
    }

    override fun onMediaClicked(position: Int) {
        Timber.d("on media clicked")
        /*container.setVisibility(View.VISIBLE);
    ((BookmarkFragment)getParentFragment()).tabLayout.setVisibility(View.GONE);
    mediaDetails = new MediaDetailPagerFragment(false, true, position);
    setFragment(mediaDetails, bookmarkPicturesFragment);*/
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     *
     * @param i It is the index of which media object is to be returned which is same as current
     * index of viewPager.
     * @return Media Object
     */
    override fun getMediaAtPosition(i: Int): Media? =
        bookmarksPagerAdapter!!.mediaAdapter?.getItem(i) as Media?

    /**
     * This method is called on from getCount of MediaDetailPagerFragment The viewpager will contain
     * same number of media items as that of media elements in adapter.
     *
     * @return Total Media count in the adapter
     */
    override fun getTotalMediaCount(): Int =
        bookmarksPagerAdapter!!.mediaAdapter?.count ?: 0

    override fun getContributionStateAt(position: Int): Int? {
        return null
    }

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (mediaDetails != null && !listFragment!!.isVisible()) {
            removeFragment(mediaDetails!!)
            mediaDetails = newInstance(false, true)
            (parentFragment as BookmarkFragment).setScroll(false)
            setFragment(mediaDetails!!, listFragment)
            mediaDetails!!.showImage(index)
        }
    }

    /**
     * This method is called on success of API call for featured images or mobile uploads. The
     * viewpager will notified that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        if (mediaDetails != null) {
            mediaDetails!!.notifyDataSetChanged()
        }
    }

    fun backPressed(): Boolean {
        //check mediaDetailPage fragment is not null then we check mediaDetail.is Visible or not to avoid NullPointerException
        if (mediaDetails != null) {
            if (mediaDetails!!.isVisible()) {
                // todo add get list fragment
                (parentFragment as BookmarkFragment).setupTabLayout()
                val removed: ArrayList<Int> = mediaDetails!!.removedItems
                removeFragment(mediaDetails!!)
                (parentFragment as BookmarkFragment).setScroll(true)
                setFragment(listFragment!!, mediaDetails)
                (requireActivity() as MainActivity).showTabs()
                if (listFragment is BookmarkPicturesFragment) {
                    val adapter = ((listFragment as BookmarkPicturesFragment)
                        .getAdapter() as GridViewAdapter?)
                    val i: MutableIterator<*> = removed.iterator()
                    while (i.hasNext()) {
                        adapter!!.remove(adapter.getItem(i.next() as Int))
                    }
                    mediaDetails!!.clearRemoved()
                }
            } else {
                moveToContributionsFragment()
            }
        } else {
            moveToContributionsFragment()
        }
        // notify mediaDetails did not handled the backPressed further actions required.
        return false
    }

    fun moveToContributionsFragment() {
        (requireActivity() as MainActivity).setSelectedItemId(NavTab.CONTRIBUTIONS.code())
        (requireActivity() as MainActivity).showTabs()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Timber.d("on media clicked")
        binding!!.exploreContainer.visibility = View.VISIBLE
        (parentFragment as BookmarkFragment).binding!!.tabLayout.setVisibility(View.GONE)
        mediaDetails = newInstance(false, true)
        (parentFragment as BookmarkFragment).setScroll(false)
        setFragment(mediaDetails!!, listFragment)
        mediaDetails!!.showImage(position)
    }

    override fun onBackStackChanged() = Unit

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
