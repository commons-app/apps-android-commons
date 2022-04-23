package fr.free.nrw.commons.explore.categories.search

import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import javax.inject.Inject

class PageableSearchCategoriesDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    val categoryClient: CategoryClient
) : PageableBaseDataSource<String>(liveDataConverter) {

    override val loadFunction = { loadSize: Int, startPosition: Int ->
        categoryClient.searchCategories(query, loadSize, startPosition).blockingGet()
            .map { it.name }
    }
}
