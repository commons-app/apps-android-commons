package fr.free.nrw.commons.explore.depictions

import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.upload.depicts.proxy
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * The presenter class for SearchDepictionsFragment
 */
class SearchDepictionsFragmentPresenter @Inject constructor(
    @param:Named(CommonsApplicationModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler,
    private val searchableDataSourceFactory: SearchableDepictionsDataSourceFactory
) : SearchDepictionsFragmentContract.UserActionListener {
    private val compositeDisposable = CompositeDisposable()
    private var view = DUMMY
    private var currentQuery: String? = null
    override val listFooterData = MutableLiveData<List<FooterItem>>().apply { value = emptyList() }

    override fun onAttachView(view: SearchDepictionsFragmentContract.View) {
        this.view = view
        compositeDisposable.addAll(
            searchableDataSourceFactory.searchResults.subscribe(view::observeSearchResults),
            searchableDataSourceFactory.loadingStates
                .observeOn(mainThreadScheduler)
                .subscribe(::onLoadingState, Timber::e),
            searchableDataSourceFactory.noItemsLoadedEvent.subscribe {
                currentQuery?.let(view::setEmptyViewText)
            }
        )
    }

    private fun onLoadingState(it: LoadingState) = when (it) {
        LoadingState.Loading -> listFooterData.postValue(listOf(FooterItem.LoadingItem))
        LoadingState.Complete -> {
            listFooterData.postValue(emptyList())
            view.hideInitialLoadProgress()
        }
        LoadingState.InitialLoad -> view.showInitialLoadInProgress()
        LoadingState.Error -> {
            currentQuery?.let(view::setEmptyViewText)
            view.showSnackbar()
            view.hideInitialLoadProgress()
            listFooterData.postValue(listOf(FooterItem.RefreshItem))
        }
    }

    override fun retryFailedRequest() {
        searchableDataSourceFactory.retryFailedRequest()
    }

    override fun onDetachView() {
        view = DUMMY
        compositeDisposable.clear()
    }

    override fun onQueryUpdated(query: String) {
        currentQuery = query
        searchableDataSourceFactory.onQueryUpdated(query)
    }

    companion object {
        private val DUMMY: SearchDepictionsFragmentContract.View = proxy()
    }
}
