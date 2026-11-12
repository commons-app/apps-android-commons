package fr.free.nrw.commons.explore.categories.search

import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.categories.PageableCategoryFragment
import fr.free.nrw.commons.utils.getStringSafe
import javax.inject.Inject

/**
 * Displays the category search screen.
 */
class SearchCategoryFragment : PageableCategoryFragment() {
    @Inject
    lateinit var presenter: SearchCategoriesFragmentPresenter

    override val injectedPresenter
        get() = presenter

    override fun getEmptyText(query: String) = requireContext().getStringSafe(R.string.categories_not_found, query)
}
