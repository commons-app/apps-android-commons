package fr.free.nrw.commons.explore

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import fr.free.nrw.commons.BasePresenter

interface SearchFragmentContract {
    interface View<T> {
        fun showSnackbar()
        fun observeSearchResults(searchResults: LiveData<PagedList<T>>)
        fun showInitialLoadInProgress()
        fun hideInitialLoadProgress()
        fun showEmptyText(query: String)
        fun hideEmptyText()
    }

    interface Presenter<T> : BasePresenter<View<T>> {
        val listFooterData: LiveData<List<FooterItem>>
        fun onQueryUpdated(query: String)
        fun retryFailedRequest()
    }
}
