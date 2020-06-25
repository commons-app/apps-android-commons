package fr.free.nrw.commons.explore.categories.search

import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.categories.PageableCategoryFragment
import javax.inject.Inject

/**
 * Displays the category search screen.
 */
class SearchCategoryFragment : PageableCategoryFragment() {
    @Inject
    lateinit var presenter: SearchCategoriesFragmentPresenter

    override val injectedPresenter
        get() = presenter

    override fun getEmptyText(query: String) = getString(R.string.categories_not_found, query)
}
