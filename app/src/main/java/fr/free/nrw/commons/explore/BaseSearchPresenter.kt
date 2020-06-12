package fr.free.nrw.commons.explore

import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.upload.depicts.proxy
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber


abstract class BaseSearchPresenter<T>(
    val mainThreadScheduler: Scheduler,
    val pageableDataSource: PageableDataSource<T>
) : SearchFragmentContract.Presenter<T> {

    private val DUMMY: SearchFragmentContract.View<T> = proxy()
    private var view: SearchFragmentContract.View<T> = DUMMY
    private var currentQuery: String? = null


    private val compositeDisposable = CompositeDisposable()
    override val listFooterData = MutableLiveData<List<FooterItem>>().apply { value = emptyList() }

    override fun onAttachView(view: SearchFragmentContract.View<T>) {
        this.view = view
        compositeDisposable.addAll(
            pageableDataSource.searchResults.subscribe(view::observeSearchResults),
            pageableDataSource.loadingStates
                .observeOn(mainThreadScheduler)
                .subscribe(::onLoadingState, Timber::e),
            pageableDataSource.noItemsLoadedEvent.subscribe {
                setEmptyViewText()
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
            setEmptyViewText()
            view.showSnackbar()
            view.hideInitialLoadProgress()
            listFooterData.postValue(listOf(FooterItem.RefreshItem))
        }
    }

    private fun setEmptyViewText() {
        currentQuery?.let(view::setEmptyViewText)
    }

    override fun retryFailedRequest() {
        pageableDataSource.retryFailedRequest()
    }

    override fun onDetachView() {
        view = DUMMY
        compositeDisposable.clear()
    }

    override fun onQueryUpdated(query: String) {
        currentQuery = query
        pageableDataSource.onQueryUpdated(query)
    }

}
