package fr.free.nrw.commons.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesFragment
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsFragment
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.databinding.FragmentFeaturedRootBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment.Companion.newInstance
import fr.free.nrw.commons.media.MediaDetailProvider
import timber.log.Timber

class BookmarkListRootFragment :
    CommonsDaggerSupportFragment,
    MediaDetailProvider,
    OnItemClickListener,
    CategoryImagesCallback {
    internal var mediaDetails: MediaDetailPagerFragment? = null
    var listFragment: Fragment? = null
    private var bookmarksPagerAdapter: BookmarksPagerAdapter? = null

    var binding: FragmentFeaturedRootBinding? = null

    /**
     * Shows and removes the child fragments of this screen and handles the back button.
     */
    val navigator = BookmarkListNavigator(this)

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
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreate(savedInstanceState)
        binding = FragmentFeaturedRootBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            navigator.setFragment(listFragment!!, mediaDetails)
        }
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
    override fun getMediaAtPosition(i: Int): Media? = bookmarksPagerAdapter!!.mediaAdapter?.getItem(i) as Media?

    /**
     * This method is called on from getCount of MediaDetailPagerFragment The viewpager will contain
     * same number of media items as that of media elements in adapter.
     *
     * @return Total Media count in the adapter
     */
    override fun getTotalMediaCount(): Int = bookmarksPagerAdapter!!.mediaAdapter?.count ?: 0

    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (mediaDetails != null && !listFragment!!.isVisible()) {
            navigator.removeFragment(mediaDetails!!)
            mediaDetails = newInstance(false, true)
            (parentFragment as BookmarkFragment).setScroll(false)
            navigator.setFragment(mediaDetails!!, listFragment)
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

    override fun onItemClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long,
    ) {
        Timber.d("on media clicked")
        binding!!.exploreContainer.visibility = View.VISIBLE
        (parentFragment as BookmarkFragment).binding!!.tabLayout.setVisibility(View.GONE)
        mediaDetails = newInstance(false, true)
        (parentFragment as BookmarkFragment).setScroll(false)
        navigator.setFragment(mediaDetails!!, listFragment)
        mediaDetails!!.showImage(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
