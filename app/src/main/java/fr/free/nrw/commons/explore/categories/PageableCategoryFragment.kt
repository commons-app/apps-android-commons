package fr.free.nrw.commons.explore.categories

import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryDetailsActivity
import fr.free.nrw.commons.explore.paging.BasePagingFragment


abstract class PageableCategoryFragment : BasePagingFragment<String>() {
    override val errorTextId: Int = R.string.error_loading_categories
    override val pagedListAdapter by lazy {
        PagedSearchCategoriesAdapter {
            CategoryDetailsActivity.startYourself(context, it)
        }
    }
}
