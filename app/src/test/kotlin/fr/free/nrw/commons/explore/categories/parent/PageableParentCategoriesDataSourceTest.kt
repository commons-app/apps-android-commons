package fr.free.nrw.commons.explore.categories.parent

import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import io.reactivex.Single
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableParentCategoriesDataSourceTest{
    @Mock
    lateinit var categoryClient: CategoryClient
    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `loadFunction calls reset at position 0`() {
        val dataSource =
            PageableParentCategoriesDataSource(liveDataConverter, categoryClient)
        dataSource.onQueryUpdated("test")
        whenever(categoryClient.getParentCategoryList("test"))
            .thenReturn(Single.just(emptyList()))
        assertThat(dataSource.loadFunction(-1, 0), `is`(emptyList()))
        verify(categoryClient).resetParentCategoryContinuation("test")
    }

    @Test
    fun `loadFunction does not call reset at any other position`() {
        val dataSource =
            PageableParentCategoriesDataSource(liveDataConverter, categoryClient)
        dataSource.onQueryUpdated("test")
        whenever(categoryClient.getParentCategoryList("test"))
            .thenReturn(Single.just(emptyList()))
        assertThat(dataSource.loadFunction(-1, 1), `is`(emptyList()))
        verify(categoryClient, never()).resetParentCategoryContinuation("test")
    }
}
