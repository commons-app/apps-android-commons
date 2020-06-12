package fr.free.nrw.commons.explore

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import fr.free.nrw.commons.BasePresenter

interface SearchFragmentContract {
    interface View<T> {
        fun showSnackbar()
        fun observeSearchResults(searchResults: LiveData<PagedList<T>>)
        fun setEmptyViewText(query: String)
        fun showInitialLoadInProgress()
        fun hideInitialLoadProgress()
    }

    interface Presenter<T> : BasePresenter<View<T>> {
        val listFooterData: LiveData<List<FooterItem>>
        fun onQueryUpdated(query: String)
        fun retryFailedRequest()
    }
}
