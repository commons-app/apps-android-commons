package fr.free.nrw.commons.explore.categroies

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.categories.search.PageableCategoriesDataSource
import io.reactivex.Observable
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Test

class PageableCategoriesDataSourceTest {
    @Test
    fun `loadFunction loads categories`() {
        val categoryClient: CategoryClient = mock()
        whenever(categoryClient.searchCategories("test", 0, 1))
            .thenReturn(Observable.just(emptyList()))
        val pageableCategoriesDataSource = PageableCategoriesDataSource(mock(), categoryClient)
        pageableCategoriesDataSource.onQueryUpdated("test")
        assertThat(pageableCategoriesDataSource.loadFunction(0, 1), Matchers.`is`(emptyList()))
    }
}
