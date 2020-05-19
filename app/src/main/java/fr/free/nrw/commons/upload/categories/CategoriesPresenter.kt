package fr.free.nrw.commons.upload.categories

import android.text.TextUtils
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.proxy
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The presenter class for UploadCategoriesFragment
 */
@Singleton
class CategoriesPresenter @Inject constructor(
    private val repository: UploadRepository,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioScheduler: Scheduler,
    @param:Named(CommonsApplicationModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler
) : CategoriesContract.UserActionListener {

    companion object {
        private val DUMMY: CategoriesContract.View = proxy()
    }

    var view = DUMMY
    private val compositeDisposable = CompositeDisposable()
    private val searchTerms = PublishSubject.create<String>()

    override fun onAttachView(view: CategoriesContract.View) {
        this.view = view
        compositeDisposable.add(
            searchTerms
                .observeOn(mainThreadScheduler)
                .doOnNext {
                    view.showProgress(true)
                    view.showError(null)
                    view.setCategories(null)
                }
                .switchMap(::searchResults)
                .map { repository.selectedCategories + it }
                .map { it.distinctBy { categoryItem -> categoryItem.name } }
                .observeOn(mainThreadScheduler)
                .subscribe(
                    {
                        view.setCategories(it)
                        view.showProgress(false)
                        if (it.isEmpty()) {
                            view.showError(R.string.no_categories_found)
                        }
                    },
                    Timber::e
                )
        )
    }

    private fun searchResults(term: String) =
        repository.searchAll(term, getImageTitleList())
            .subscribeOn(ioScheduler)
            .map { it.filterNot { categoryItem -> repository.containsYear(categoryItem.name) } }

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
     * Returns image title list from UploadItem
     * @return
     */
    private fun getImageTitleList(): List<String> {
        return repository.uploads
            .map { it.uploadMediaDetails[0].captionText }
            .filterNot { TextUtils.isEmpty(it) }
    }

    /**
     * Verifies the number of categories selected, prompts the user if none selected
     */
    override fun verifyCategories() {
        val selectedCategories = repository.selectedCategories
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
        repository.onCategoryClicked(categoryItem)
    }
}
