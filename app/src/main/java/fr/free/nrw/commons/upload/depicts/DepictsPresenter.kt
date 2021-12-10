package fr.free.nrw.commons.upload.depicts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.WikidataDisambiguationItems
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.reflect.Proxy
import java.util.*
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
    @Inject
    lateinit var depictsDao: DepictsDao
    @Inject
    lateinit var depictsHelper: DepictEditHelper

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

    private fun searchResults(querystring: String): Flowable<List<DepictedItem>> {
        var recentDepictedItemList: MutableList<DepictedItem> = ArrayList();
        //show recentDepictedItemList when queryString is empty
        if (querystring.isEmpty()) {
            recentDepictedItemList = getRecentDepictedItems();
        }
        return repository.searchAllEntities(querystring)
            .subscribeOn(ioScheduler)
            .map { repository.selectedDepictions + it + recentDepictedItemList }
            .map { it.filterNot { item -> WikidataDisambiguationItems.isDisambiguationItem(item.instanceOfs) } }
            .map { it.distinctBy(DepictedItem::id) }
    }


    override fun onDetachView() {
        view = DUMMY
        compositeDisposable.clear()
    }

    /**
     * Selects the place depictions retrieved by the repository
     */
    override fun selectPlaceDepictions() {
        compositeDisposable.add(repository.placeDepictions
            .subscribeOn(ioScheduler)
            .observeOn(mainThreadScheduler)
            .subscribe(::selectNewDepictions)
        )
    }

    /**
     * Selects each [DepictedItem] in a given list as if they were clicked by the user by calling
     * [onDepictItemClicked] for each depiction and adding the depictions to [depictedItems]
     */
    private fun selectNewDepictions(toSelect: List<DepictedItem>) {
        toSelect.forEach {
            it.isSelected = true
            repository.onDepictItemClicked(it)
        }

        // Add the new selections to the list of depicted items so that the selections appear
        // immediately (i.e. without any search term queries)
        depictedItems.value?.toMutableList()
            ?.let { toSelect + it }
            ?.distinctBy(DepictedItem::id)
            ?.let { depictedItems.value = it }
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
            if (::depictsDao.isInitialized) {
                //save all the selected Depicted item in room Database
                depictsDao.savingDepictsInRoomDataBase(repository.selectedDepictions)
            }
            view.goToNextScreen()
        } else {
            view.noDepictionSelected()
        }
    }

    /**
     * Gets the selected depicts and send them for posting to the server
     * and saves them in local storage
     */
    override fun updateDepicts(media: Media) {
        view.showProgressDialog()
        if (repository.selectedDepictions.isNotEmpty()) {
            val selectedDepictions: MutableList<String> =
                repository.selectedDepictions.map { it.id }.toMutableList()

            if (selectedDepictions.isNotEmpty()) {
                for (depicts in selectedDepictions) {
                    if (media.depictionIds.contains(depicts)) {
                        selectedDepictions.remove(depicts)
                    }
                }
                val allDepicts = media.depictionIds.plus(selectedDepictions)
                if (::depictsDao.isInitialized) {
                    //save all the selected Depicted item in room Database
                    depictsDao.savingDepictsInRoomDataBase(repository.selectedDepictions)
                }

                compositeDisposable.add(depictsHelper.makeDepictEdit(
                    view.fragmentContext,
                    media,
                    selectedDepictions
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        media.depictionIds = allDepicts
                        view.goBackToPreviousScreen()
                        view.dismissProgressDialog()
                    }) {
                        Timber.e(
                            "Failed to update depicts"
                        )
                    }
                )
            } else {
                view.noDepictionSelected()
            }
        }
    }

    /**
     * Get the depicts from DepictsRoomdataBase
     */
    fun getRecentDepictedItems(): MutableList<DepictedItem> {
        val depictedItemList: MutableList<DepictedItem> = ArrayList()
        val depictsList = depictsDao.depictsList()
        for (i in depictsList.indices) {
            val depictedItem = depictsList[i].item
            depictedItemList.add(depictedItem)
        }
        return depictedItemList
    }
}

/**
 * This creates a dynamic proxy instance of the class,
 * proxy is to control access to the target object
 * here our target object is the view.
 * Thus we when onDettach method of fragment is called we replace the binding of view to our object with the proxy instance
 */
inline fun <reified T> proxy() = Proxy
    .newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> null } as T
