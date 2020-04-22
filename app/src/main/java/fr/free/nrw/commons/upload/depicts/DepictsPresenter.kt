package fr.free.nrw.commons.upload.depicts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
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
    @param:Named(CommonsApplicationModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler,
    private val depictsClient: DepictsClient
) : DepictsContract.UserActionListener {

    companion object {
        private val DUMMY = proxy<DepictsContract.View>()
    }

    private var view = DUMMY
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val searchTerm: PublishProcessor<String> = PublishProcessor.create()
    val depictedItems: MutableLiveData<List<DepictedItem>> = MutableLiveData()
    private val idsToImageUrls = mutableMapOf<String, String>()

    init {
        compositeDisposable.add(
            searchTerm
                .observeOn(mainThreadScheduler)
                .doOnNext { view.showProgress(true) }
                .observeOn(ioScheduler)
                .switchMap(repository::searchAllEntities)
                .map { it.distinct() }
                .map(::addImageUrlsFromCache)
                .observeOn(mainThreadScheduler)
                .subscribe(
                    {
                        view.showProgress(false)
                        view.showError(it.isEmpty())
                        depictedItems.value = it
                    },
                    { t: Throwable? ->
                        view.showProgress(false)
                        view.showError(true)
                        Timber.e(t)
                    }
                )
        )
    }

    private fun addImageUrlsFromCache(depictions: List<DepictedItem>) =
        depictions.map { item ->
            idsToImageUrls.getOrElse(item.id, { null })
                ?.let { item.copy(imageUrl = it) }
                ?: item
        }

    override fun onAttachView(view: DepictsContract.View) {
        this.view = view
    }

    override fun onDetachView() {
        view = DUMMY
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
        val selectedDepictions =
            repository.selectedDepictions
        if (selectedDepictions != null && !selectedDepictions.isEmpty()) {
            view.goToNextScreen()
        } else {
            view.noDepictionSelected()
        }
    }

    /**
     * Fetch thumbnail for the Wikidata Item
     * @param entityId entityId of the item
     * @param position position of the item
     */
    override fun fetchThumbnailForEntityId(depictedItem: DepictedItem) {
        compositeDisposable.add(
            imageUrlFromNetworkOrCache(depictedItem)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response: String? ->
                    if (response != null) {
                        depictedItems.value =
                            depictedItems.value!!.map {
                                if (it.id == depictedItem.id) it.copy(imageUrl = response)
                                else it
                            }
                    }
                }
        )
    }

    private fun imageUrlFromNetworkOrCache(depictedItem: DepictedItem) =
        if (idsToImageUrls.containsKey(depictedItem.imageUrl))
            Single.just(idsToImageUrls[depictedItem.id])
        else
            depictsClient.getP18ForItem(depictedItem.id)
                .subscribeOn(Schedulers.io())
                .doOnSuccess { idsToImageUrls[depictedItem.id] = it }
}

inline fun <reified T> proxy() = Proxy
    .newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> null } as T
