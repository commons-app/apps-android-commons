package fr.free.nrw.commons.explore.depictions

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.MergeAdapter
import fr.free.nrw.commons.R
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.ViewUtil
import kotlinx.android.synthetic.main.fragment_search_depictions.*
import javax.inject.Inject

/**
 * Display depictions in search fragment
 */
class SearchDepictionsFragment : CommonsDaggerSupportFragment(),
    SearchDepictionsFragmentContract.View {

    @Inject
    lateinit var presenter: SearchDepictionsFragmentContract.UserActionListener

    private val depictionsAdapter by lazy {
        DepictionAdapter { WikidataItemDetailsActivity.startYourself(context, it) }
    }
    private val loadingAdapter by lazy { FooterAdapter { presenter.retryFailedRequest() } }
    private val mergeAdapter by lazy { MergeAdapter(depictionsAdapter, loadingAdapter) }

    var searchResults: LiveData<PagedList<DepictedItem>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_search_depictions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        depictionsSearchResultsList.apply {
            layoutManager = GridLayoutManager(context, if (isPortrait) 1 else 2)
            adapter = mergeAdapter
        }
        presenter.listFooterData.observe(viewLifecycleOwner, Observer(loadingAdapter::submitList))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presenter.onAttachView(this)
    }

    override fun onDetach() {
        super.onDetach()
        presenter.onDetachView()
    }

    override fun observeSearchResults(searchResults: LiveData<PagedList<DepictedItem>>) {
        this.searchResults?.removeObservers(viewLifecycleOwner)
        this.searchResults = searchResults
        searchResults.observe(viewLifecycleOwner, Observer {
            depictionsAdapter.submitList(it)
            depictionNotFound.visibility = if (it.loadedCount == 0) VISIBLE else GONE
        })
    }

    override fun setEmptyViewText(query: String) {
        depictionNotFound.text = getString(R.string.depictions_not_found, query)
    }

    override fun hideInitialLoadProgress() {
        depictionSearchInitialLoadProgress.visibility = GONE
    }

    override fun showInitialLoadInProgress() {
        depictionSearchInitialLoadProgress.visibility = VISIBLE
    }

    override fun showSnackbar() {
        ViewUtil.showShortSnackbar(depictionsSearchResultsList, R.string.error_loading_depictions)
    }

    fun onQueryUpdated(query: String) {
        presenter.onQueryUpdated(query)
    }
}

private val Fragment.isPortrait get() = orientation == Configuration.ORIENTATION_PORTRAIT

private val Fragment.orientation get() = activity!!.resources.configuration.orientation
