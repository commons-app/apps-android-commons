package fr.free.nrw.commons.explore.categories.parent

import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import javax.inject.Inject

class PageableParentCategoriesDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    val categoryClient: CategoryClient
) : PageableBaseDataSource<String>(liveDataConverter) {

    override val loadFunction = { loadSize: Int, startPosition: Int ->
        if (startPosition == 0) {
            categoryClient.resetParentCategoryContinuation(query)
        }
        categoryClient.getParentCategoryList(query).blockingGet().map { it.name }
    }
}
