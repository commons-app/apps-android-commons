package fr.free.nrw.commons.upload.categories

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.UploadActivity
import io.reactivex.Notification
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeUnit

class CategorySearchView : CategoriesContract.SearchView {
    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 500L
    }

    internal var adapter: UploadCategoryAdapter? = null
    internal var subscribe: Disposable? = null

    internal lateinit var fragment: UploadCategoriesFragment

    fun addTextChangeListenerToEtSearch() {
        if (fragment.binding == null) {
            return
        }
        subscribe =
            RxTextView
                .textChanges(fragment.binding!!.etSearch)
                .doOnEach { v: Notification<CharSequence?>? ->
                    fragment.binding?.tilContainerSearch?.error =
                        null
                }.takeUntil(RxView.detaches(fragment.binding!!.etSearch))
                .debounce(SEARCH_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { filter: CharSequence -> searchForCategory(filter.toString()) },
                    { t: Throwable? -> Timber.e(t) },
                )
    }

    private fun searchForCategory(query: String) {
        fragment.presenter?.searchForCategories(query)
    }

    fun initRecyclerView() {
        if (adapter == null) {
            adapter =
                UploadCategoryAdapter({ categoryItem: CategoryItem? ->
                    fragment.presenter?.onCategoryItemClicked(categoryItem!!)
                    Unit
                }, fragment.nearbyPlaceCategory)
        }
        fragment.binding?.rvCategories?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CategorySearchView.adapter
            clearOnScrollListeners()
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) {
                        if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                            fragment.presenter?.loadMoreCategories()
                        }
                    }
                },
            )
        }
    }

    fun dispose() {
        subscribe?.dispose()
    }

    override fun showProgress(shouldShow: Boolean) {
        fragment.binding?.pbCategories?.setVisibility(if (shouldShow) View.VISIBLE else View.GONE)
    }

    override fun showError(error: String?) {
        fragment.binding?.tilContainerSearch?.error = error
    }

    override fun showError(stringResourceId: Int) {
        fragment.binding?.tilContainerSearch?.error = fragment.getString(stringResourceId)
    }

    override fun showErrorDialog(message: String) {
        AlertDialog
            .Builder(fragment.requireContext())
            .setMessage(fragment.getString(R.string.error_loading_categories) + "\n" + message)
            .setCancelable(false)
            .setNegativeButton(R.string.ok) { _, _ -> }
            .show()
    }

    override fun setCategories(categories: List<CategoryItem>?) {
        if (adapter == null) {
            Timber.e("Adapter is null in setCategories")
            return
        }

        val shown = adapter!!.items.orEmpty()
        val isAppend =
            categories != null &&
                shown.isNotEmpty() &&
                categories.size > shown.size &&
                categories.subList(0, shown.size) == shown

        if (categories == null) {
            adapter!!.clear()
        } else {
            adapter!!.items = categories
        }
        adapter!!.notifyDataSetChanged()

        fragment.binding?.let {
            it.rvCategories.post {
                // Also load more when the list is too short to scroll, e.g. no prefix results at all
                if (!it.rvCategories.canScrollVertically(1)) {
                    fragment.presenter?.loadMoreCategories()
                }
            }
            if (isAppend) {
                // More results were added at the end, so stay where the user scrolled to
                return
            }
            it.rvCategories.post {
                it.rvCategories.smoothScrollToPosition(0)
                it.rvCategories.post {
                    it.rvCategories.smoothScrollToPosition(
                        0,
                    )
                }
            }
        } ?: Timber.e("Binding is null in setCategories")
    }

    /**
     * Removes  the tv subtitle If the activity is the instance of [UploadActivity] and
     * if multiple files aren't selected.
     */
    fun setTvSubTitle() {
        val activity: Activity? = fragment.activity
        if (activity is UploadActivity) {
            val isMultipleFileSelected = activity.isMultipleFilesSelected
            if (!isMultipleFileSelected) {
                fragment.binding!!.tvSubtitle.visibility = View.GONE
            }
        }
    }
}
