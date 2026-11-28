package fr.free.nrw.commons.upload.depicts

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsController
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.IO_THREAD
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.MAIN_THREAD
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.WikidataDisambiguationItems
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * presenter for DepictsFragment
 */
@Singleton
class DepictsPresenter
    @Inject
    constructor(
        private val repository: UploadRepository,
        @param:Named(IO_THREAD) private val ioScheduler: Scheduler,
        @param:Named(MAIN_THREAD) private val mainThreadScheduler: Scheduler,
        private val session: DepictsSession = DepictsSession(),
    ) : DepictsContract.UserActionListener,
        DepictsContract.SelectionActions by DepictsSelectionHandler(
            repository,
            session,
            ioScheduler,
            mainThreadScheduler,
        ) {
        companion object {
            private val DUMMY = proxy<DepictsContract.View>()
        }

        private var view = DUMMY
        private val compositeDisposable get() = session.compositeDisposable
        private val searchTerm: PublishProcessor<String> = PublishProcessor.create()
        private val depictedItems get() = session.depictedItems
        private var media: Media? by session::media

        @Inject
        lateinit var depictsDao: DepictsDao

        /**
         * Helps to get all bookmarked items
         */
        @Inject
        lateinit var controller: BookmarkItemsController

        @Inject
        lateinit var depictsHelper: DepictEditHelper

        /**
         * Builds the search results. It reads [controller] and [depictsDao] each time they are
         * needed, exactly like the presenter did before the search logic was moved.
         */
        private val searchHandler =
            DepictsSearchHandler(repository, { controller }, { depictsDao }, ioScheduler)

        override fun onAttachView(view: DepictsContract.View) {
            this.view = view
            compositeDisposable.add(
                searchTerm
                    .observeOn(mainThreadScheduler)
                    .doOnNext { view.showProgress(true) }
                    .switchMap { term -> searchHandler.searchResultsWithTerm(term, media) }
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
                        },
                    ),
            )
        }

        override fun onDetachView() {
            view = DUMMY
            media = null
            compositeDisposable.clear()
        }

        override fun getUploads(): List<UploadItem> = repository.getUploads()

        override fun onPreviousButtonClicked() {
            view.goToPreviousScreen()
        }

        override fun onSetAsLocationOfCreation(depictedItem: DepictedItem) {
            // Depictions and location are independent: a picture can show the place where it
            // was taken, so an item that is already selected as a depiction stays selected
            repository.setLocationOfCreation(depictedItem)
            view.showLocationOfCreation(depictedItem)
        }

        override fun onClearLocationOfCreation() {
            repository.setLocationOfCreation(null)
            view.showLocationOfCreation(null)
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
            if (repository.getSelectedDepictions().isNotEmpty()) {
                if (::depictsDao.isInitialized) {
                    // save all the selected Depicted item in room Database
                    depictsDao.savingDepictsInRoomDataBase(repository.getSelectedDepictions())
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
        @SuppressLint("CheckResult")
        override fun updateDepictions(media: Media) {
            if (repository.getSelectedDepictions().isNotEmpty() ||
                repository.getSelectedExistingDepictions().size != view.getExistingDepictions()?.size
            ) {
                view.showProgressDialog()
                val selectedDepictions: MutableList<String> =
                    (
                        repository.getSelectedDepictions().map { it.id }.toMutableList() +
                            repository.getSelectedExistingDepictions()
                    ).toMutableList()

                if (selectedDepictions.isNotEmpty()) {
                    if (::depictsDao.isInitialized) {
                        // save all the selected Depicted item in room Database
                        depictsDao.savingDepictsInRoomDataBase(repository.getSelectedDepictions())
                    }

                    compositeDisposable.add(
                        depictsHelper
                            .makeDepictionEdit(view.getFragmentContext(), media, selectedDepictions)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                media.depictionIds = selectedDepictions
                                repository.cleanup()
                                view.dismissProgressDialog()
                                view.updateDepicts()
                                view.goBackToPreviousScreen()
                            }, { error ->
                                if (error is InvalidLoginTokenException) {
                                    view.navigateToLoginScreen()
                                } else {
                                    view.dismissProgressDialog()
                                    Timber.e("Failed to update depictions")
                                }
                            }),
                    )
                }
            } else {
                repository.cleanup()
                view.noDepictionSelected()
            }
        }

        override fun onAttachViewWithMedia(
            view: DepictsContract.View,
            media: Media,
        ) {
            this.view = view
            this.media = media
            repository.setSelectedExistingDepictions(view.getExistingDepictions().orEmpty())
            compositeDisposable.add(
                searchTerm
                    .observeOn(mainThreadScheduler)
                    .doOnNext { view.showProgress(true) }
                    .switchMap { term -> searchHandler.searchResultsWithTerm(term, this.media) }
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
                        },
                    ),
            )
        }
    }

/**
 * Holds the state that [DepictsPresenter] shares with [DepictsSelectionHandler], so that both
 * always work on the same media, depicted items and disposables.
 */
class DepictsSession
    @Inject
    constructor() {
        /**
         * Disposables of every subscription started by the depicts presenter
         */
        val compositeDisposable: CompositeDisposable = CompositeDisposable()

        /**
         * The depicted items currently shown to the user
         */
        val depictedItems: MutableLiveData<List<DepictedItem>> = MutableLiveData()

        /**
         * The media whose depictions are being edited, or null while uploading
         */
        var media: Media? = null
    }

/**
 * Handles selecting depictions for [DepictsPresenter]. The logic was moved out of the presenter
 * without any behaviour change and works on the state held by [DepictsSession].
 *
 * @property repository source of the place depictions and keeper of the selected depictions
 * @property session state shared with the presenter
 * @property ioScheduler scheduler on which the depictions are fetched
 * @property mainThreadScheduler scheduler on which the fetched depictions are selected
 */
class DepictsSelectionHandler(
    private val repository: UploadRepository,
    private val session: DepictsSession,
    private val ioScheduler: Scheduler,
    private val mainThreadScheduler: Scheduler,
) : DepictsContract.SelectionActions {
    /**
     * Selects the place depictions retrieved by the repository
     */
    override fun selectPlaceDepictions() {
        session.compositeDisposable.add(
            repository
                .getPlaceDepictions()
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .subscribe(::selectNewDepictions),
        )
    }

    /**
     * Selects each [DepictedItem] in a given list as if they were clicked by the user by calling
     * [onDepictItemClicked] for each depiction and adding the depictions to the depicted items
     */
    private fun selectNewDepictions(toSelect: List<DepictedItem>) {
        toSelect.forEach {
            it.isSelected = true
            repository.onDepictItemClicked(it, session.media)
        }

        // Add the new selections to the list of depicted items so that the selections appear
        // immediately (i.e. without any search term queries)
        session.depictedItems.value
            ?.toMutableList()
            ?.let { toSelect + it }
            ?.distinctBy(DepictedItem::id)
            ?.let { session.depictedItems.value = it }
    }

    /**
     * Clears the selections kept by the repository
     */
    override fun clearPreviousSelection() {
        repository.cleanup()
    }

    /**
     * Update Depictions list which are selected from the map
     */
    override fun onPlaceSelectedFromMap(place: Place) {
        place.wikiDataEntityId?.let { qid ->
            session.compositeDisposable.add(
                repository
                    .getDepictions(listOf(qid))
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({ depictedItems ->
                        selectNewDepictions(depictedItems)
                    }, { error ->
                        Timber.e(error, "Failed to fetch depiction for place from map")
                    }),
            )
        }
    }

    /**
     * Passes a clicked depicted item to the repository together with the current media
     */
    override fun onDepictItemClicked(depictedItem: DepictedItem) {
        repository.onDepictItemClicked(depictedItem, session.media)
    }

    /**
     * Gets the depicted items currently shown to the user
     */
    override fun getDepictedItems(): LiveData<List<DepictedItem>> = session.depictedItems

    /** Reads the selected photo location from the current upload. */
    override fun getLocationOfCreation(): DepictedItem? = repository.getLocationOfCreation()
}

/**
 * Builds the depiction search results shown in the depicts step of the upload flow.
 *
 * It combines the repository search with the already selected depictions, the recently used
 * depictions and the bookmarked depictions. The logic was moved out of [DepictsPresenter]
 * without any behaviour change.
 *
 * @property repository source of search results and of the currently selected depictions
 * @property controller provides the bookmarked depiction items when they are needed
 * @property depictsDao provides the recently used depictions when they are needed
 * @property ioScheduler scheduler on which the search is subscribed
 */
class DepictsSearchHandler(
    private val repository: UploadRepository,
    private val controller: () -> BookmarkItemsController,
    private val depictsDao: () -> DepictsDao,
    private val ioScheduler: Scheduler,
) {
    /**
     * Searches for depictions and pairs every emitted result list with the term that produced it.
     *
     * @param term the search term
     * @param media the media whose depictions are being edited, or null while uploading
     * @return a flowable of the results together with [term]
     */
    fun searchResultsWithTerm(
        term: String,
        media: Media?,
    ): Flowable<Pair<List<DepictedItem>, String>> = searchResults(term, media).map { Pair(it, term) }

    /**
     * Searches for depictions matching [querystring]. The result contains the selected,
     * found, recent (only for an empty query) and bookmarked items, without disambiguation
     * items and without duplicates.
     *
     * @param querystring the search query
     * @param media the media whose depictions are being edited, or null while uploading
     * @return a flowable of the matching depicted items
     */
    fun searchResults(
        querystring: String,
        media: Media?,
    ): Flowable<List<DepictedItem>> {
        var recentDepictedItemList: MutableList<DepictedItem> = ArrayList()
        // show recentDepictedItemList when queryString is empty
        if (querystring.isEmpty()) {
            recentDepictedItemList = getRecentDepictedItems().toMutableList()
        }

        if (media == null) {
            return repository
                .searchAllEntities(querystring)
                .subscribeOn(ioScheduler)
                .map {
                    repository.getSelectedDepictions() + it + recentDepictedItemList +
                        controller().loadFavoritesItems()
                }.map {
                    it.filterNot { item ->
                        WikidataDisambiguationItems
                            .isDisambiguationItem(item.instanceOfs)
                    }
                }.map { it.distinctBy(DepictedItem::id) }
        } else {
            return Flowable
                .zip(
                    repository
                        .getDepictions(repository.getSelectedExistingDepictions())
                        .map { list ->
                            list.map {
                                DepictedItem(
                                    it.name,
                                    it.description,
                                    it.imageUrl,
                                    it.instanceOfs,
                                    it.commonsCategories,
                                    true,
                                    it.id,
                                )
                            }
                        },
                    repository.searchAllEntities(querystring),
                ) { it1, it2 ->
                    it1 + it2
                }.subscribeOn(ioScheduler)
                .map {
                    repository.getSelectedDepictions() + it + recentDepictedItemList +
                        controller().loadFavoritesItems()
                }.map {
                    it.filterNot { item ->
                        WikidataDisambiguationItems
                            .isDisambiguationItem(item.instanceOfs)
                    }
                }.map { it.distinctBy(DepictedItem::id) }
        }
    }

    /**
     * Get the depicts from DepictsRoomdataBase
     */
    private fun getRecentDepictedItems(): List<DepictedItem> =
        runBlocking {
            val depictsList = depictsDao().depictsList().await()
            return@runBlocking depictsList.map { it.item }
        }
}

/**
 * This creates a dynamic proxy instance of the class,
 * proxy is to control access to the target object
 * here our target object is the view.
 * Thus we when onDettach method of fragment is called we replace the binding of view to our object with the proxy
 * instance
 */
inline fun <reified T> proxy() =
    Proxy
        .newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> null } as T
