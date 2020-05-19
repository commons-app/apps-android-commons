package fr.free.nrw.commons.explore.depictions

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

/**
 * The contract with with SearchDepictionsFragment and its presenter would talk to each other
 */
interface SearchDepictionsFragmentContract {
    interface View {
        fun showSnackbar()
        fun observeSearchResults(searchResults: LiveData<PagedList<DepictedItem>>)
        fun setEmptyViewText(query: String)
        fun showInitialLoadInProgress()
        fun hideInitialLoadProgress()
    }

    interface UserActionListener : BasePresenter<View?> {
        val listFooterData: LiveData<List<FooterItem>>
        fun onQueryUpdated(query: String)
        fun retryFailedRequest()
    }
}
