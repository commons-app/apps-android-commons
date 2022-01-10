package fr.free.nrw.commons.explore.paging

import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.upload.depicts.proxy
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber


abstract class BasePagingPresenter<T>(
    val mainThreadScheduler: Scheduler,
    val pageableBaseDataSource: PageableBaseDataSource<T>
) : PagingContract.Presenter<T> {

    private val DUMMY: PagingContract.View<T> = proxy()
    private var view: PagingContract.View<T> = DUMMY

    private val compositeDisposable = CompositeDisposable()
    override val listFooterData = MutableLiveData<List<FooterItem>>().apply { value = emptyList() }

    override fun onAttachView(view: PagingContract.View<T>) {
        this.view = view
        compositeDisposable.addAll(
            pageableBaseDataSource.pagingResults.subscribe(view::observePagingResults),
            pageableBaseDataSource.loadingStates
                .observeOn(mainThreadScheduler)
                .subscribe(::onLoadingState, Timber::e),
            pageableBaseDataSource.noItemsLoadedQueries.subscribe(view::showEmptyText)
        )
    }

    private fun onLoadingState(it: LoadingState) = when (it) {
        LoadingState.Loading -> {
            view.hideEmptyText()
            listFooterData.postValue(listOf(FooterItem.LoadingItem))
        }
        LoadingState.Complete -> {
            listFooterData.postValue(emptyList())
            view.hideInitialLoadProgress()
        }
        LoadingState.InitialLoad -> {
            view.hideEmptyText()
            view.showInitialLoadInProgress()
        }
        LoadingState.Error -> {
            view.showSnackbar()
            view.hideInitialLoadProgress()
            listFooterData.postValue(listOf(FooterItem.RefreshItem))
        }
    }

    override fun retryFailedRequest() {
        pageableBaseDataSource.retryFailedRequest()
    }

    override fun onDetachView() {
        view = DUMMY
        compositeDisposable.clear()
    }

    override fun onQueryUpdated(query: String) {
        pageableBaseDataSource.onQueryUpdated(query)
    }
}
