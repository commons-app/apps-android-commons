package fr.free.nrw.commons.explore.paging

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentSearchPaginatedBinding
import fr.free.nrw.commons.utils.ViewUtil

/**
 * Owns paging observations and loading feedback while the fragment retains its lifecycle callbacks.
 * View-dependent values are resolved when used so recreating the fragment view uses its new owner.
 */
internal class PagingView<T> : PagingContract.View<T> {
    private lateinit var fragment: BasePagingFragment<T>
    private lateinit var binding: () -> FragmentSearchPaginatedBinding
    private var searchResults: LiveData<PagedList<T>>? = null

    /**
     * Connects the delegate without accessing a binding or lifecycle owner before view creation.
     */
    fun bind(
        fragment: BasePagingFragment<T>,
        binding: () -> FragmentSearchPaginatedBinding,
    ) {
        this.fragment = fragment
        this.binding = binding
    }

    /**
     * Replaces the previous query's observer using the lifecycle of the current fragment view.
     */
    override fun observePagingResults(searchResults: LiveData<PagedList<T>>) {
        this.searchResults?.removeObservers(fragment.viewLifecycleOwner)
        this.searchResults = searchResults
        searchResults.observe(
            fragment.viewLifecycleOwner,
            Observer {
                fragment.pagedListAdapter.submitList(it)
            },
        )
    }

    /** Hides the initial loader without changing the paging footer's separate loading state. */
    override fun hideInitialLoadProgress() {
        binding().paginatedSearchInitialLoadProgress.visibility = GONE
    }

    /** Displays the initial loader while the first page is being fetched. */
    override fun showInitialLoadInProgress() {
        binding().paginatedSearchInitialLoadProgress.visibility = VISIBLE
    }

    /** Anchors paging errors above the activity's navigation tabs when those tabs are available. */
    override fun showSnackbar() {
        val anchorView = fragment.activity?.findViewById<View>(R.id.fragment_main_nav_tab_layout)
        ViewUtil.showShortSnackbar(binding().paginatedSearchResultsList, fragment.errorTextId, anchorView)
    }

    /** Uses the fragment's result-specific message when a query has no matching items. */
    override fun showEmptyText(query: String) {
        binding().contentNotFound.text = fragment.getEmptyText(query)
        binding().contentNotFound.visibility = VISIBLE
    }

    /** Removes the previous query's empty-state message when another load starts. */
    override fun hideEmptyText() {
        binding().contentNotFound.visibility = GONE
    }
}
