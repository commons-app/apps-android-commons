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
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The presenter class for UploadCategoriesFragment
 */
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
            compositeDisposable.add(
                searchTerms
                    .observeOn(mainThreadScheduler)
                    .doOnNext {
                        view.showProgress(true)
                    }.switchMap { searchResults.searchResults(it, media) }
                    .map { repository.getSelectedCategories() + it }
                    .map { it.distinctBy { categoryItem -> categoryItem.name } }
                    .observeOn(mainThreadScheduler)
                    .subscribe(
                        {
                            categoryList.postValue(it)
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

        override fun onDetachView() {
            view = DUMMY
            compositeDisposable.clear()
        }

        /**
         * asks the repository to fetch categories for the query
         * @param query
         */
        override fun searchForCategories(query: String) {
            searchTerms.onNext(query)
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
                    }.switchMap { searchResults.searchResults(it, media) }
                    .map { repository.getSelectedCategories() + it }
                    .map { it.distinctBy { categoryItem -> categoryItem.name } }
                    .observeOn(mainThreadScheduler)
                    .subscribe(
                        {
                            categoryList.postValue(it)
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
                                .subscribe({
                                    Timber.d("Categories are added.")
                                    media.addedCategories = selectedCategories
                                    repository.cleanup()
                                    view.dismissProgressDialog()
                                    view.refreshCategories()
                                    view.goBackToPreviousScreen()
                                }, {
                                    Timber.e(
                                        "Failed to update categories",
                                    )
                                }),
                        )
                    } catch (e: InvalidLoginTokenException) {
                        Timber.e(e, "Invalid login token while updating categories")
                        view.navigateToLoginScreen()
                    }
                }
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
                    .subscribe { categorySelection.selectNewCategories(it, media, categoryList) },
            )
        }
    }
