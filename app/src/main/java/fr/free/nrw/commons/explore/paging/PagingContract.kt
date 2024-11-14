package fr.free.nrw.commons.explore.paging

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import fr.free.nrw.commons.BasePresenter

interface PagingContract {
    interface View<T: Any> {
        fun showSnackbar()

        fun observePagingResults(searchResults: LiveData<PagedList<T>>)

        fun showInitialLoadInProgress()

        fun hideInitialLoadProgress()

        fun showEmptyText(query: String)

        fun hideEmptyText()
    }

    interface Presenter<T: Any> : BasePresenter<View<T>> {
        val listFooterData: LiveData<List<FooterItem>>

        fun onQueryUpdated(query: String)

        fun retryFailedRequest()
    }
}
