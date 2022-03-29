package fr.free.nrw.commons.explore.paging

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.MergeAdapter
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.maps.MapView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.utils.ViewUtil
import kotlinx.android.synthetic.main.fragment_search_paginated.*


abstract class BasePagingMapFragment<T> : CommonsDaggerSupportFragment(),
    PagingMapContract.View<T> {

    abstract val pagedListAdapter: PagedListAdapter<T, *>
    //abstract val injectedPresenter: PagingMapContract.Presenter<T>
    abstract val errorTextId: Int
    @BindView(R.id.map_view) lateinit var mapView: MapView
    @BindView(R.id.bottom_sheet_details) lateinit var bottomSheetDetails: View
    @BindView(R.id.map_progress_bar) lateinit var progressBar: ProgressBar
    @BindView(R.id.fab_recenter) lateinit var fabRecenter: FloatingActionButton
    @BindView(R.id.search_this_area_button) lateinit var searchThisAreaButton: Button
    @BindView(R.id.tv_attribution) lateinit var tvAttribution: AppCompatTextView

    @BindView(R.id.bookmarkButtonImage) lateinit var bookmarkButtonImage: ImageView
    @BindView(R.id.bookmarkButton) lateinit var bookmarkButton: LinearLayout
    @BindView(R.id.wikipediaButton) lateinit var wikipediaButton: LinearLayout
    @BindView(R.id.wikidataButton) lateinit var wikidataButton: LinearLayout
    @BindView(R.id.directionsButton) lateinit var directionsButton: LinearLayout
    @BindView(R.id.commonsButton) lateinit var commonsButton: LinearLayout
    @BindView(R.id.description) lateinit var description: TextView
    @BindView(R.id.title) lateinit var title: TextView
    @BindView(R.id.category) lateinit var distance: TextView

    //private val loadingAdapter by lazy { FooterAdapter { injectedPresenter.retryFailedRequest() } }
    //private val mergeAdapter by lazy { MergeAdapter(pagedListAdapter, loadingAdapter) }
    private var searchResults: LiveData<PagedList<T>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_search_map_paginated, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*paginatedSearchResultsList.apply {
            layoutManager = GridLayoutManager(context, if (isPortrait) 1 else 2)
            adapter = mergeAdapter
        }*/
        // TODO: if parent activity is search activity
            ButterKnife.bind(this, view!!)
            mapView.onStart();
        //if (activity is SearchActivity) {
            /*injectedPresenter.listFooterData.observe(
                viewLifecycleOwner,
                Observer(loadingAdapter::submitList)
            )*/
        //}
         // Else it is a general explore activity
    }

    protected abstract fun getLayoutResource(): Int

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
            /*injectedPresenter?.let {
                injectedPresenter.onAttachView(this)
            }*/
    }

    override fun onDetach() {
        super.onDetach()
        // injectedPresenter.onDetachView()
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

    fun onQueryUpdated(query: String, isFromSearchActivity: Boolean) {

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
