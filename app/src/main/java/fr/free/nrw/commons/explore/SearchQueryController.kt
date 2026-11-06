package fr.free.nrw.commons.explore

import android.text.TextUtils
import android.view.View
import androidx.fragment.app.FragmentManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxSearchView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.databinding.ActivitySearchBinding
import fr.free.nrw.commons.explore.categories.search.SearchCategoryFragment
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragment
import fr.free.nrw.commons.explore.media.SearchMediaFragment
import fr.free.nrw.commons.explore.models.RecentSearch
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment
import fr.free.nrw.commons.utils.FragmentUtils.isFragmentUIActive
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

private const val SEARCH_DEBOUNCE_MILLIS = 500L

/**
 * Owns the search result tabs and recent-query history while the activity handles media navigation.
 * The DAO is resolved when a query arrives, after the activity's dependencies have been injected.
 */
internal class SearchQueryController(
    private val binding: ActivitySearchBinding,
    private val fragmentManager: FragmentManager,
    private val recentSearchesDao: () -> RecentSearchesDao,
) {
    var searchMediaFragment: SearchMediaFragment? = null
        private set
    private var searchCategoryFragment: SearchCategoryFragment? = null
    private var searchDepictionsFragment: SearchDepictionsFragment? = null
    private var recentSearchesFragment: RecentSearchesFragment? = null

    /**
     * This method sets the search history fragment.
     * Search history fragment is displayed when query is empty.
     */
    fun setSearchHistoryFragment() {
        recentSearchesFragment = RecentSearchesFragment()
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.searchHistoryContainer, recentSearchesFragment!!).commit()
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    fun setTabs(
        viewPagerAdapter: ViewPagerAdapter,
        compositeDisposable: CompositeDisposable,
    ) {
        searchMediaFragment = SearchMediaFragment()
        searchDepictionsFragment = SearchDepictionsFragment()
        searchCategoryFragment = SearchCategoryFragment()

        viewPagerAdapter.setTabs(
            R.string.search_tab_title_media to searchMediaFragment!!,
            R.string.search_tab_title_categories to searchCategoryFragment!!,
            R.string.search_tab_title_depictions to searchDepictionsFragment!!,
        )
        viewPagerAdapter.notifyDataSetChanged()
        compositeDisposable.add(
            RxSearchView
                .queryTextChanges(binding.searchBox)
                .takeUntil(RxView.detaches(binding.searchBox))
                .debounce(SEARCH_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::handleSearch, Timber::e),
        )
    }

    /**
     * Switches between history and results, sending nonempty queries only to active result views.
     */
    fun handleSearch(query: CharSequence) {
        if (!TextUtils.isEmpty(query)) {
            saveRecentSearch(query.toString())
            binding.viewPager.visibility = View.VISIBLE
            binding.tabLayout.visibility = View.VISIBLE
            binding.searchHistoryContainer.visibility = View.GONE

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
            // Open RecentSearchesFragment
            recentSearchesFragment!!.updateRecentSearches()
            binding.viewPager.visibility = View.GONE
            binding.tabLayout.visibility = View.GONE
            setSearchHistoryFragment()
            binding.searchHistoryContainer.visibility = View.VISIBLE
        }
    }

    /**
     * Reuses an existing history entry when possible so repeating a query updates its timestamp.
     */
    private fun saveRecentSearch(query: String) {
        val dao = recentSearchesDao()
        val recentSearch = dao.find(query)
        // Newly searched query...
        if (recentSearch == null) {
            dao.save(RecentSearch(null, query, Date()))
        } else {
            recentSearch.lastSearched = Date()
            dao.save(recentSearch)
        }
    }
}
