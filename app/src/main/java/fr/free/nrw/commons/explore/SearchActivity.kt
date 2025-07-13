package fr.free.nrw.commons.explore

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.FragmentManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxSearchView
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.databinding.ActivitySearchBinding
import fr.free.nrw.commons.explore.categories.search.SearchCategoryFragment
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragment
import fr.free.nrw.commons.explore.media.SearchMediaFragment
import fr.free.nrw.commons.explore.models.RecentSearch
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment.MediaDetailProvider
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.FragmentUtils.isFragmentUIActive
import fr.free.nrw.commons.utils.ViewUtil.hideKeyboard
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Represents search screen of this app
 */
class SearchActivity : BaseActivity(), MediaDetailProvider, CategoryImagesCallback {
    @JvmField
    @Inject
    var recentSearchesDao: RecentSearchesDao? = null

    private var searchMediaFragment: SearchMediaFragment? = null
    private var searchCategoryFragment: SearchCategoryFragment? = null
    private var searchDepictionsFragment: SearchDepictionsFragment? = null
    private var recentSearchesFragment: RecentSearchesFragment? = null
    private var supportFragmentManager: FragmentManager? = null
    private var mediaDetails: MediaDetailPagerFragment? = null
    private var viewPagerAdapter: ViewPagerAdapter? = null
    private var binding: ActivitySearchBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        title = getString(R.string.title_activity_search)
        setSupportActionBar(binding!!.toolbarSearch)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding!!.toolbarSearch.setNavigationOnClickListener { onBackPressed() }
        supportFragmentManager = getSupportFragmentManager()
        setSearchHistoryFragment()
        viewPagerAdapter = ViewPagerAdapter(this, getSupportFragmentManager())
        binding!!.viewPager.adapter = viewPagerAdapter
        binding!!.viewPager.offscreenPageLimit = 2 // Because we want all the fragments to be alive
        binding!!.tabLayout.setupWithViewPager(binding!!.viewPager)
        setTabs()
        binding!!.searchBox.queryHint = getString(R.string.search_commons)
        binding!!.searchBox.onActionViewExpanded()
        binding!!.searchBox.clearFocus()
    }

    /**
     * This method sets the search history fragment.
     * Search history fragment is displayed when query is empty.
     */
    private fun setSearchHistoryFragment() {
        recentSearchesFragment = RecentSearchesFragment()
        val transaction = supportFragmentManager!!.beginTransaction()
        transaction.add(R.id.searchHistoryContainer, recentSearchesFragment!!).commit()
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    fun setTabs() {
        searchMediaFragment = SearchMediaFragment()
        searchDepictionsFragment = SearchDepictionsFragment()
        searchCategoryFragment = SearchCategoryFragment()

        viewPagerAdapter!!.setTabs(
            R.string.search_tab_title_media to searchMediaFragment!!,
            R.string.search_tab_title_categories to searchCategoryFragment!!,
            R.string.search_tab_title_depictions to searchDepictionsFragment!!
        )
        viewPagerAdapter!!.notifyDataSetChanged()
        compositeDisposable.add(
            RxSearchView.queryTextChanges(binding!!.searchBox)
                .takeUntil(RxView.detaches(binding!!.searchBox))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::handleSearch, Timber::e)
        )
    }

    private fun handleSearch(query: CharSequence) {
        if (!TextUtils.isEmpty(query)) {
            saveRecentSearch(query.toString())
            binding!!.viewPager.visibility = View.VISIBLE
            binding!!.tabLayout.visibility = View.VISIBLE
            binding!!.searchHistoryContainer.visibility = View.GONE

            if (isFragmentUIActive(searchDepictionsFragment)) {
                searchDepictionsFragment!!.onQueryUpdated(query.toString())
            }

            if (isFragmentUIActive(searchMediaFragment)) {
                searchMediaFragment!!.onQueryUpdated(query.toString())
            }

            if (isFragmentUIActive(searchCategoryFragment)) {
                searchCategoryFragment!!.onQueryUpdated(query.toString())
            }
        } else {
            //Open RecentSearchesFragment
            recentSearchesFragment!!.updateRecentSearches()
            binding!!.viewPager.visibility = View.GONE
            binding!!.tabLayout.visibility = View.GONE
            setSearchHistoryFragment()
            binding!!.searchHistoryContainer.visibility = View.VISIBLE
        }
    }

    private fun saveRecentSearch(query: String) {
        val recentSearch = recentSearchesDao!!.find(query)
        // Newly searched query...
        if (recentSearch == null) {
            recentSearchesDao!!.save(RecentSearch(null, query, Date()))
        } else {
            recentSearch.lastSearched = Date()
            recentSearchesDao!!.save(recentSearch)
        }
    }

    override fun getMediaAtPosition(i: Int): Media? = searchMediaFragment!!.getMediaAtPosition(i)

    override fun getTotalMediaCount(): Int = searchMediaFragment!!.totalMediaCount

    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (getSupportFragmentManager().backStackEntryCount == 1) {
            onBackPressed()
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

    /**
     * Open media detail pager fragment on click of image in search results
     * @param position item index that should be opened
     */
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
            //FIXME: Temporary fix for screen rotation inside media details. If we don't call onBackPressed then fragment stack is increasing every time.
            //FIXME: Similar issue like this https://github.com/commons-app/apps-android-commons/issues/894
            // This is called on screen rotation when user is inside media details. Ideally it should show Media Details but since we are not saving the state now. We are throwing the user to search screen otherwise the app was crashing.
            onBackPressed()
        }
        super.onResume()
    }

    /**
     * This method is called on backPressed of anyFragment in the activity.
     * If condition is called when mediaDetailFragment is opened.
     */
    override fun onBackPressed() {
        //Remove the backstack entry that gets added when share button is clicked
        //fixing:https://github.com/commons-app/apps-android-commons/issues/2296
        if (getSupportFragmentManager().backStackEntryCount == 2) {
            supportFragmentManager!!
                .beginTransaction()
                .remove(mediaDetails!!)
                .commit()
            supportFragmentManager!!.popBackStack()
            supportFragmentManager!!.executePendingTransactions()
        }
        if (getSupportFragmentManager().backStackEntryCount == 1) {
            // back to search so show search toolbar and hide navigation toolbar
            binding!!.searchBox.visibility = View.VISIBLE //set the searchview
            binding!!.tabLayout.visibility = View.VISIBLE
            binding!!.viewPager.visibility = View.VISIBLE
            binding!!.mediaContainer.visibility = View.GONE
        } else {
            binding!!.toolbarSearch.visibility = View.GONE
        }
        super.onBackPressed()
    }

    /**
     * This method is called on click of a recent search to update query in SearchView.
     * @param query Recent Search Query
     */
    fun updateText(query: String?) {
        binding!!.searchBox.setQuery(query, true)
        // Clear focus of searchView now. searchView.clearFocus(); does not seem to work Check the below link for more details.
        // https://stackoverflow.com/questions/6117967/how-to-remove-focus-without-setting-focus-to-another-control/15481511
        binding!!.viewPager.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Dispose the disposables when the activity is destroyed
        compositeDisposable.dispose()
        binding = null
    }
}
