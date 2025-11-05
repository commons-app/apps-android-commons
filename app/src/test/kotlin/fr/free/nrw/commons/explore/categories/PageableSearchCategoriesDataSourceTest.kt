package fr.free.nrw.commons.explore.categories

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.categories.search.PageableSearchCategoriesDataSource
import io.reactivex.Single
import org.junit.Assert
import org.junit.Test

class PageableSearchCategoriesDataSourceTest {
    @Test
    fun `loadFunction loads categories`() {
        val categoryClient: CategoryClient = mock()
        whenever(categoryClient.searchCategories("test", 0, 1))
            .thenReturn(Single.just(emptyList()))
        val pageableCategoriesDataSource =
            PageableSearchCategoriesDataSource(mock(), categoryClient)
        pageableCategoriesDataSource.onQueryUpdated("test")
        Assert.assertEquals(pageableCategoriesDataSource.loadFunction(0, 1), emptyList<String>())
    }
}
