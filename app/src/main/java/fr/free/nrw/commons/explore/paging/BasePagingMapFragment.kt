package fr.free.nrw.commons.explore.paging

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.MergeAdapter
import fr.free.nrw.commons.R
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.utils.ViewUtil
import kotlinx.android.synthetic.main.fragment_search_paginated.*


abstract class BasePagingMapFragment<T> : CommonsDaggerSupportFragment(),
    PagingContract.View<T> {

    abstract val pagedListAdapter: PagedListAdapter<T, *>
    abstract val injectedPresenter: PagingContract.Presenter<T>
    abstract val errorTextId: Int
    private val loadingAdapter by lazy { FooterAdapter { injectedPresenter.retryFailedRequest() } }
    private val mergeAdapter by lazy { MergeAdapter(pagedListAdapter, loadingAdapter) }
    private var searchResults: LiveData<PagedList<T>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_search_map_paginated, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*paginatedSearchResultsList.apply {
            layoutManager = GridLayoutManager(context, if (isPortrait) 1 else 2)
            adapter = mergeAdapter
        }*/
        injectedPresenter.listFooterData.observe(
            viewLifecycleOwner,
            Observer(loadingAdapter::submitList)
        )
    }

    /**
     * Called on configuration change, update the spanCount according to the orientation state.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        /*paginatedSearchResultsList.apply {
            layoutManager = GridLayoutManager(context, if (isPortrait) 1 else 2)
        }*/
    }

    override fun observePagingResults(searchResults: LiveData<PagedList<T>>) {
        this.searchResults?.removeObservers(viewLifecycleOwner)
        this.searchResults = searchResults
        searchResults.observe(viewLifecycleOwner, Observer {
            pagedListAdapter.submitList(it)
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        injectedPresenter?.let {
            injectedPresenter.onAttachView(this)
        }
    }

    override fun onDetach() {
        super.onDetach()
        injectedPresenter.onDetachView()
    }

    override fun hideInitialLoadProgress() {
        paginatedSearchInitialLoadProgress.visibility = GONE
    }

    override fun showInitialLoadInProgress() {
        paginatedSearchInitialLoadProgress.visibility = VISIBLE
    }

    override fun showSnackbar() {
        ViewUtil.showShortSnackbar(paginatedSearchResultsList, errorTextId)
    }

    fun onQueryUpdated(query: String) {
        injectedPresenter.onQueryUpdated(query)
    }

    override fun showEmptyText(query: String) {
        contentNotFound.text = getEmptyText(query)
        contentNotFound.visibility = VISIBLE
    }

    abstract fun getEmptyText(query: String): String

    override fun hideEmptyText() {
        contentNotFound.visibility = GONE
    }
}

private val Fragment.isPortrait get() = orientation == Configuration.ORIENTATION_PORTRAIT

private val Fragment.orientation get() = activity!!.resources.configuration.orientation
