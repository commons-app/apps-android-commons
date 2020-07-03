package fr.free.nrw.commons.explore.categroies

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.categories.search.PageableSearchCategoriesDataSource
import io.reactivex.Single
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
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
        assertThat(pageableCategoriesDataSource.loadFunction(0, 1), Matchers.`is`(emptyList()))
    }
}
