package fr.free.nrw.commons.explore.categories

import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryDetailsActivity
import fr.free.nrw.commons.explore.paging.BasePagingFragment
import javax.inject.Inject

/**
 * Displays the category search screen.
 */
class SearchCategoryFragment : BasePagingFragment<String>() {
    @Inject
    lateinit var presenter: SearchCategoriesFragmentPresenter

    override val errorTextId: Int = R.string.error_loading_categories

    override val injectedPresenter
        get() = presenter

    override val pagedListAdapter by lazy {
        PagedSearchCategoriesAdapter { CategoryDetailsActivity.startYourself(context, it) }
    }

    override fun getEmptyText(query: String) = getString(R.string.categories_not_found, query)
}
