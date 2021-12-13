package fr.free.nrw.commons.upload.categories

import android.text.TextUtils
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryEditHelper
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.proxy
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.ArrayList
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

    /**
     * Helper class for editing categories
     */
    @Inject
    lateinit var categoryEditHelper: CategoryEditHelper
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
        repository.searchAll(term, getImageTitleList(), repository.selectedDepictions)
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
     * Take the categories selected and merge them with old categories and update those in the
     * commons server
     *
     * @param media Media of edited categories
     */
    override fun updateCategories(media: Media) {
        view.showProgressDialog()
        val selectedCategories: MutableList<String> = repository.selectedCategories.map { it.name }.toMutableList()
        if (selectedCategories.isNotEmpty()) {
            for (category in selectedCategories){
                if(media.categories?.contains(category) == true){
                    selectedCategories.remove(category)
                }
            }
            val allCategories = media.categories?.plus(selectedCategories)
            compositeDisposable.add(categoryEditHelper.makeCategoryEdit(
                view.fragmentContext,
                media,
                allCategories
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    media.addedCategories = selectedCategories
                    updateCategoryList(media)
                    view.goBackToPreviousScreen()
                    view.dismissProgressDialog()
                }) {
                    Timber.e(
                        "Failed to update categories"
                    )
                }
            )
        } else {
            view.showNoCategorySelected()
        }
    }

    private fun updateCategoryList(media: Media) {
        val allCategories: MutableList<String> = ArrayList<String>(media.categories)
        if (media.addedCategories != null) {
            // TODO this added categories logic should be removed.
            //  It is just a short term hack. Categories should be fetch everytime they are updated.
            // if media.getCategories contains addedCategory, then do not re-add them
            for (addedCategory in media.addedCategories!!) {
                if (allCategories.contains(addedCategory)) {
                    media.addedCategories = null
                    break
                }
            }
            allCategories.addAll(media.addedCategories!!)
        }
        if (allCategories.isEmpty()) {
            // Stick in a filler element.
            allCategories.add(view.fragmentContext.getString(R.string.detail_panel_cats_none))
        }
        rebuildCatList(allCategories)
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
