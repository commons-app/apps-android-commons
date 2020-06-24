package fr.free.nrw.commons.explore.categories

import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.LiveDataConverter
import fr.free.nrw.commons.explore.PageableDataSource
import javax.inject.Inject

class PageableCategoriesDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    val categoryClient: CategoryClient
) : PageableDataSource<String>(liveDataConverter) {

    override val loadFunction = { loadSize: Int, startPosition: Int ->
        categoryClient.searchCategories(query, loadSize, startPosition).blockingFirst()
    }
}
