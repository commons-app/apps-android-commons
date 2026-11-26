package fr.free.nrw.commons.upload.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.category.CategoryEditHelper
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.IO_THREAD
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.MAIN_THREAD
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.proxy
import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The presenter class for UploadCategoriesFragment.
 * TooManyFunctions is suppressed because every function is one action from the categories screen.
 */
@Suppress("TooManyFunctions")
@Singleton
class CategoriesPresenter
    @Inject
    constructor(
        private val repository: UploadRepository,
        @param:Named(IO_THREAD) private val ioScheduler: Scheduler,
        @param:Named(MAIN_THREAD) private val mainThreadScheduler: Scheduler,
    ) : CategoriesContract.UserActionListener {
        companion object {
            private val DUMMY: CategoriesContract.View = proxy()
        }

        var view = DUMMY
        private val compositeDisposable = CompositeDisposable()
        private val searchResults = CategorySearchResults(repository, ioScheduler)
        private val categorySelection = CategorySelection(repository)
        private val searchTerms = PublishSubject.create<String>()
        internal val categoryList: MutableLiveData<List<CategoryItem>> = MutableLiveData()

        /**
         * The typed search whose results are shown, empty while a new search loads
         */
        private var shownTerm = ""

        /**
         * The request for the next page of results, if one is running
         */
        private var loadMoreRequest: Disposable? = null

        /**
         * Current media
         */
        private var media: Media? = null

        /**
         * helper class for editing categories
         */
        @Inject
        lateinit var categoryEditHelper: CategoryEditHelper

        override fun onAttachView(view: CategoriesContract.View) {
            this.view = view

            fun showLoadError(t: Throwable?) {
                view.showProgress(false)
                view.showError(R.string.error_loading_categories)
                val mwException = t as? MwIOException
                view.showErrorDialog(
                    if (mwException == null) {
                        ""
                    } else {
                        "\n${mwException.error.title} / ${mwException.error.details}"
                    },
                )
                Timber.e(t)
            }

            compositeDisposable.add(
                searchTerms
                    .observeOn(mainThreadScheduler)
                    .doOnNext {
                        view.showProgress(true)
                        startNewSearch()
                    }.switchMap { term ->
                        searchResults
                            .searchResults(term, media)
                            .map { Result.success(term to it) }
                            .onErrorReturn { Result.failure(it) }
                    }.map { result ->
                        result.map { (term, found) ->
                            term to
                                (repository.getSelectedCategories() + found)
                                    .distinctBy { categoryItem -> categoryItem.name }
                        }
                    }.observeOn(mainThreadScheduler)
                    .subscribe(
                        { result ->
                            view.showProgress(false)
                            result.fold(
                                onSuccess = { (term, it) ->
                                    showResults(term, it)
                                    if (it.isEmpty()) {
                                        view.showError(R.string.no_categories_found)
                                    }
                                },
                                onFailure = ::showLoadError,
                            )
                        },
                        ::showLoadError,
                    ),
            )
        }

        override fun onDetachView() {
            view = DUMMY
            compositeDisposable.clear()
            startNewSearch()
        }

        /**
         * Forgets the shown search and cancels loading its next page
         */
        private fun startNewSearch() {
            shownTerm = ""
            loadMoreRequest?.dispose()
        }

        /**
         * Shows the results of a search. Both are set at once, so a next page is never added to
         * the results of another search.
         */
        private fun showResults(
            term: String,
            categories: List<CategoryItem>,
        ) {
            shownTerm = term
            categoryList.value = categories
        }

        /**
         * asks the repository to fetch categories for the query
         * @param query
         */
        override fun searchForCategories(query: String) {
            searchTerms.onNext(query)
        }

        override fun loadMoreCategories() {
            val term = shownTerm
            if (term.isEmpty() || loadMoreRequest?.isDisposed == false) {
                return
            }
            loadMoreRequest =
                searchResults
                    .moreResults(term)
                    .observeOn(mainThreadScheduler)
                    .subscribe(
                        { found ->
                            if (term != shownTerm) {
                                return@subscribe
                            }
                            val shown = categoryList.value.orEmpty()
                            val merged = (shown + found).distinctBy { it.name }
                            if (merged.size > shown.size) {
                                view.showError(null)
                                categoryList.value = merged
                            } else if (found.isNotEmpty()) {
                                // Everything on this page is already shown, so get the next one
                                loadMoreCategories()
                            }
                        },
                        { t: Throwable -> Timber.e(t) },
                    )
        }

        /**
         * Verifies the number of categories selected, prompts the user if none selected
         */
        override fun verifyCategories() {
            val selectedCategories = repository.getSelectedCategories()
            if (selectedCategories.isNotEmpty()) {
                repository.setSelectedCategories(selectedCategories.map { it.name })
                view.goToNextScreen()
            } else {
                view.showNoCategorySelected()
            }
        }

        /**
         * ask repository to handle category clicked
         *
         * @param categoryItem
         */
        override fun onCategoryItemClicked(categoryItem: CategoryItem) {
            repository.onCategoryClicked(categoryItem, media)
        }

        /**
         * Attaches view and media
         */
        override fun onAttachViewWithMedia(
            view: CategoriesContract.View,
            media: Media,
        ) {
            this.view = view
            this.media = media
            repository.setSelectedExistingCategories(view.getExistingCategories() ?: emptyList())
            compositeDisposable.add(
                searchTerms
                    .observeOn(mainThreadScheduler)
                    .doOnNext {
                        view.showProgress(true)
                        startNewSearch()
                    }.switchMap { term ->
                        searchResults.searchResults(term, media).map { term to it }
                    }.map { (term, found) ->
                        term to (repository.getSelectedCategories() + found).distinctBy { it.name }
                    }.observeOn(mainThreadScheduler)
                    .subscribe(
                        { (term, it) ->
                            showResults(term, it)
                            view.showProgress(false)
                            if (it.isEmpty()) {
                                view.showError(R.string.no_categories_found)
                            }
                        },
                        { t: Throwable? ->
                            view.showProgress(false)
                            view.showError(R.string.error_loading_categories)
                            val mwException = t as? MwIOException
                            view.showErrorDialog(
                                if (mwException == null) {
                                    ""
                                } else {
                                    "\n${mwException.error.title} / ${mwException.error.details}"
                                },
                            )
                            Timber.e(t)
                        },
                    ),
            )
        }

        /**
         * Clears previous selections
         */
        override fun clearPreviousSelection() {
            repository.cleanup()
        }

        /**
         * Gets the selected categories and send them for posting to the server
         *
         * @param media media
         * @param wikiText current WikiText from server
         */
        override fun updateCategories(
            media: Media,
            wikiText: String,
        ) {
            // check if view.existingCategories is null
            if (
                repository.getSelectedCategories().isNotEmpty() ||
                (
                    view.getExistingCategories() != null &&
                        repository.getSelectedExistingCategories().size
                        !=
                        view.getExistingCategories()?.size
                )
            ) {
                val selectedCategories: MutableList<String> =
                    (
                        repository.getSelectedCategories().map { it.name }.toMutableList() +
                            repository.getSelectedExistingCategories()
                    ).toMutableList()

                if (selectedCategories.isNotEmpty()) {
                    view.showProgressDialog()

                    try {
                        compositeDisposable.add(
                            categoryEditHelper
                                .makeCategoryEdit(
                                    view.getFragmentContext(),
                                    media,
                                    selectedCategories,
                                    wikiText,
                                ).subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ saved ->
                                    view.dismissProgressDialog()
                                    if (saved) {
                                        Timber.d("Categories are added.")
                                        // Clear the selection only once the editor is closing, so
                                        // it can never stay open with an empty selection
                                        view.goBackToPreviousScreen()
                                        repository.cleanup()
                                        view.refreshCategories(selectedCategories)
                                    } else {
                                        view.showError(R.string.category_edit_helper_edit_message_else)
                                    }
                                }, {
                                    Timber.e(
                                        "Failed to update categories",
                                    )
                                    view.dismissProgressDialog()
                                    view.showError(R.string.category_edit_helper_edit_message_else)
                                }),
                        )
                    } catch (e: InvalidLoginTokenException) {
                        Timber.e(e, "Invalid login token while updating categories")
                        view.navigateToLoginScreen()
                    }
                }
            } else if (repository.getSelectedExistingCategories().isNotEmpty()) {
                // Nothing changed and categories are still selected, so there is
                // nothing to save: close the editor without a warning
                view.goBackToPreviousScreen()
                repository.cleanup()
            } else {
                repository.cleanup()
                view.showNoCategorySelected()
            }
        }

        /**
         * Livedata being used to observe category list inside
         * @see UploadCategoriesFragment
         * Any changes to category list reflect immediately to the adapter list
         */
        override fun getCategories(): LiveData<List<CategoryItem>> = categoryList

        override fun selectCategories() {
            compositeDisposable.add(
                repository
                    .getPlaceCategories()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe(
                        { categorySelection.selectNewCategories(it, media, categoryList) },
                        { t: Throwable -> Timber.e(t) },
                    ),
            )
        }
    }
