package fr.free.nrw.commons.explore

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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


abstract class BaseSearchFragment<T> : CommonsDaggerSupportFragment(),
    SearchFragmentContract.View<T> {

    abstract val pagedListAdapter: PagedListAdapter<T, *>
    abstract val injectedPresenter: SearchFragmentContract.Presenter<T>
    abstract val errorTextId: Int
    private val loadingAdapter by lazy { FooterAdapter { injectedPresenter.retryFailedRequest() } }
    private val mergeAdapter by lazy { MergeAdapter(pagedListAdapter, loadingAdapter) }
    private var searchResults: LiveData<PagedList<T>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_search_paginated, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paginatedSearchResultsList.apply {
            layoutManager = GridLayoutManager(context, if (isPortrait) 1 else 2)
            adapter = mergeAdapter
        }
        injectedPresenter.listFooterData.observe(
            viewLifecycleOwner,
            Observer(loadingAdapter::submitList)
        )
    }

    override fun observeSearchResults(searchResults: LiveData<PagedList<T>>) {
        this.searchResults?.removeObservers(viewLifecycleOwner)
        this.searchResults = searchResults
        searchResults.observe(viewLifecycleOwner, Observer {
            pagedListAdapter.submitList(it) })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        injectedPresenter.onAttachView(this)
    }

    override fun onDetach() {
        super.onDetach()
        injectedPresenter.onDetachView()
    }

    override fun hideInitialLoadProgress() {
        paginatedSearchInitialLoadProgress.visibility = View.GONE
    }

    override fun showInitialLoadInProgress() {
        paginatedSearchInitialLoadProgress.visibility = View.VISIBLE
    }

    override fun showSnackbar() {
        ViewUtil.showShortSnackbar(paginatedSearchResultsList, errorTextId)
    }

    fun onQueryUpdated(query: String) {
        injectedPresenter.onQueryUpdated(query)
    }

    override fun showEmptyText(query: String) {
        contentNotFound.text = getEmptyText(query)
        contentNotFound.visibility = View.VISIBLE
    }

    abstract fun getEmptyText(query: String):String

    override fun hideEmptyText() {
        contentNotFound.visibility = View.GONE
    }
}

private val Fragment.isPortrait get() = orientation == Configuration.ORIENTATION_PORTRAIT

private val Fragment.orientation get() = activity!!.resources.configuration.orientation
