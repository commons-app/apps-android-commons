package fr.free.nrw.commons.upload.depicts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.WikidataDisambiguationItems
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import timber.log.Timber
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * presenter for DepictsFragment
 */
@Singleton
class DepictsPresenter @Inject constructor(
    private val repository: UploadRepository,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioScheduler: Scheduler,
    @param:Named(CommonsApplicationModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler
) : DepictsContract.UserActionListener {

    companion object {
        private val DUMMY = proxy<DepictsContract.View>()
    }

    private var view = DUMMY
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val searchTerm: PublishProcessor<String> = PublishProcessor.create()
    private val depictedItems: MutableLiveData<List<DepictedItem>> = MutableLiveData()

    override fun onAttachView(view: DepictsContract.View) {
        this.view = view
        compositeDisposable.add(
            searchTerm
                .observeOn(mainThreadScheduler)
                .doOnNext { view.showProgress(true) }
                .switchMap(::searchResultsWithTerm)
                .observeOn(mainThreadScheduler)
                .subscribe(
                    { (results, term) ->
                        view.showProgress(false)
                        view.showError(results.isEmpty() && term.isNotEmpty())
                        depictedItems.value = results
                    },
                    { t: Throwable? ->
                        view.showProgress(false)
                        view.showError(true)
                        Timber.e(t)
                    }
                )
        )
    }

    private fun searchResultsWithTerm(term: String): Flowable<Pair<List<DepictedItem>, String>> {
        return searchResults(term).map { Pair(it, term) }
    }

    private fun searchResults(it: String): Flowable<List<DepictedItem>> {
        return repository.searchAllEntities(it)
            .subscribeOn(ioScheduler)
            .map { repository.selectedDepictions + it }
            .map { it.filterNot { item -> WikidataDisambiguationItems.isDisambiguationItem(item.instanceOfs) } }
            .map { it.distinctBy(DepictedItem::id) }
    }


    override fun onDetachView() {
        view = DUMMY
        compositeDisposable.clear()
    }

    override fun onPreviousButtonClicked() {
        view.goToPreviousScreen()
    }

    override fun onDepictItemClicked(depictedItem: DepictedItem) {
        repository.onDepictItemClicked(depictedItem)
    }

    override fun getDepictedItems(): LiveData<List<DepictedItem>> {
        return depictedItems;
    }

    /**
     * asks the repository to fetch depictions for the query
     * @param query
     */
    override fun searchForDepictions(query: String) {
        searchTerm.onNext(query)
    }

    /**
     * Check if depictions were selected
     * from the depiction list
     */
    override fun verifyDepictions() {
        if (repository.selectedDepictions.isNotEmpty()) {
            view.goToNextScreen()
        } else {
            view.noDepictionSelected()
        }
    }

}

inline fun <reified T> proxy() = Proxy
    .newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> null } as T
