package fr.free.nrw.commons.explore.categories

import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryDetailsActivity
import fr.free.nrw.commons.explore.BaseSearchFragment
import fr.free.nrw.commons.explore.SearchFragmentContract
import javax.inject.Inject

/**
 * Displays the category search screen.
 */
class SearchCategoryFragment : BaseSearchFragment<String>() {
    @Inject
    lateinit var presenter: SearchCategoriesFragmentContract.Presenter

    override val errorTextId: Int = R.string.error_loading_categories

    override val injectedPresenter: SearchFragmentContract.Presenter<String>
        get() = presenter

    override val pagedListAdapter by lazy {
        PagedSearchCategoriesAdapter { CategoryDetailsActivity.startYourself(context, it) }
    }

    override fun getEmptyText(query: String) = getString(R.string.categories_not_found, query)
}
