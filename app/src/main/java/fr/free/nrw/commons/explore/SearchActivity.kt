package fr.free.nrw.commons.explore

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.databinding.ActivitySearchBinding
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.ViewUtil.hideKeyboard
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import javax.inject.Inject

/**
 * Represents search screen of this app
 */
class SearchActivity :
    BaseActivity(),
    MediaDetailProvider,
    CategoryImagesCallback {
    @JvmField
    @Inject
    var recentSearchesDao: RecentSearchesDao? = null

    private var supportFragmentManager: FragmentManager? = null
    private var mediaDetails: MediaDetailPagerFragment? = null
    private var viewPagerAdapter: ViewPagerAdapter? = null
    private var binding: ActivitySearchBinding? = null
    private lateinit var searchQueryController: SearchQueryController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        applyEdgeToEdgeAllInsets(binding!!.root)
        setContentView(binding!!.root)

        title = getString(R.string.title_activity_search)
        setSupportActionBar(binding!!.toolbarSearch)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding!!.toolbarSearch.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportFragmentManager = getSupportFragmentManager()
        searchQueryController =
            SearchQueryController(binding!!, supportFragmentManager!!) { recentSearchesDao!! }
        searchQueryController.setSearchHistoryFragment()
        viewPagerAdapter = ViewPagerAdapter(this, getSupportFragmentManager())
        binding!!.viewPager.adapter = viewPagerAdapter
        binding!!.viewPager.offscreenPageLimit = 2 // Because we want all the fragments to be alive
        binding!!.tabLayout.setupWithViewPager(binding!!.viewPager)
        searchQueryController.setTabs(viewPagerAdapter!!, compositeDisposable)
        binding!!.searchBox.queryHint = getString(R.string.search_commons)
        binding!!.searchBox.onActionViewExpanded()
        binding!!.searchBox.clearFocus()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Remove the backstack entry that gets added when share button is clicked
                    // fixing:https://github.com/commons-app/apps-android-commons/issues/2296
                    if (supportFragmentManager!!.backStackEntryCount == 2) {
                        supportFragmentManager!!
                            .beginTransaction()
                            .remove(mediaDetails!!)
                            .commit()
                        supportFragmentManager!!.popBackStack()
                        supportFragmentManager!!.executePendingTransactions()
                    }
                    if (supportFragmentManager!!.backStackEntryCount == 1) {
                        // back to search so show search toolbar and hide navigation toolbar
                        binding!!.searchBox.visibility = View.VISIBLE // set the searchview
                        binding!!.tabLayout.visibility = View.VISIBLE
                        binding!!.viewPager.visibility = View.VISIBLE
                        binding!!.mediaContainer.visibility = View.GONE
                    } else {
                        binding!!.toolbarSearch.visibility = View.GONE
                    }
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            },
        )
    }

    override fun getMediaAtPosition(i: Int): Media? = searchQueryController.searchMediaFragment!!.getMediaAtPosition(i)

    override fun getTotalMediaCount(): Int = searchQueryController.searchMediaFragment!!.getTotalMediaCount()

    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (getSupportFragmentManager().backStackEntryCount == 1) {
            onBackPressedDispatcher.onBackPressed()
            onMediaClicked(index)
        }
    }

    /**
     * This method is called on success of API call for image Search.
     * The viewpager will notified that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        mediaDetails?.notifyDataSetChanged()
    }

    // Keep the reference URL intact for copy/paste.

    /**
     * Open media detail pager fragment on click of image in search results
     * @param position item index that should be opened
     */
    @Suppress("MaxLineLength")
    override fun onMediaClicked(position: Int) {
        hideKeyboard(findViewById(R.id.searchBox))
        binding!!.tabLayout.visibility = View.GONE
        binding!!.viewPager.visibility = View.GONE
        binding!!.mediaContainer.visibility = View.VISIBLE
        binding!!.searchBox.visibility =
            View.GONE // to remove searchview when mediaDetails fragment open
        if (mediaDetails == null || !mediaDetails!!.isVisible) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = MediaDetailPagerFragment.newInstance(false, true)
            supportFragmentManager!!
                .beginTransaction()
                .hide(supportFragmentManager!!.fragments[supportFragmentManager!!.backStackEntryCount])
                .add(R.id.mediaContainer, mediaDetails!!)
                .addToBackStack(null)
                .commit()
            // Reason for using hide, add instead of replace is to maintain scroll position after
            // coming back to the search activity. See https://github.com/commons-app/apps-android-commons/issues/1631
            // https://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack/19022550#19022550
            supportFragmentManager!!.executePendingTransactions()
        }
        mediaDetails!!.showImage(position)
    }

    /**
     * This method is called on Screen Rotation
     */
    override fun onResume() {
        if (supportFragmentManager!!.backStackEntryCount == 1) {
            // FIXME: Temporary fix for screen rotation inside media details. If we don't call onBackPressed then
            // fragment stack is increasing every time.
            // FIXME: Similar issue like this https://github.com/commons-app/apps-android-commons/issues/894
            // This is called on screen rotation when user is inside media details. Ideally it should show Media
            // Details but since we are not saving the state now. We are throwing the user to search screen otherwise
            // the app was crashing.
            onBackPressedDispatcher.onBackPressed()
        }
        super.onResume()
    }

    // Keep the reference URL intact for copy/paste.

    /**
     * This method is called on click of a recent search to update query in SearchView.
     * @param query Recent Search Query
     */
    @Suppress("MaxLineLength")
    fun updateText(query: String?) {
        binding!!.searchBox.setQuery(query, true)
        // Clear focus of searchView now. searchView.clearFocus(); does not seem to work Check the below link for more
        // details.
        // https://stackoverflow.com/questions/6117967/how-to-remove-focus-without-setting-focus-to-another-control/15481511
        binding!!.viewPager.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dispose the disposables when the activity is destroyed
        compositeDisposable.dispose()
        binding = null
    }
}
