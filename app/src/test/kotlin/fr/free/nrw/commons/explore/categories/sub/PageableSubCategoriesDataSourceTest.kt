package fr.free.nrw.commons.explore.categories.sub

import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableSubCategoriesDataSourceTest{
    @Mock
    lateinit var categoryClient: CategoryClient
    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadFunction calls reset at position 0`() {
        val dataSource =
            PageableSubCategoriesDataSource(liveDataConverter, categoryClient)
        dataSource.onQueryUpdated("test")
        whenever(categoryClient.getSubCategoryList("test"))
            .thenReturn(Single.just(emptyList()))
        Assert.assertEquals(dataSource.loadFunction(-1, 0), emptyList<String>())
        verify(categoryClient).resetSubCategoryContinuation("test")
    }

    @Test
    fun `loadFunction does not call reset at any other position`() {
        val dataSource =
            PageableSubCategoriesDataSource(liveDataConverter, categoryClient)
        dataSource.onQueryUpdated("test")
        whenever(categoryClient.getSubCategoryList("test"))
            .thenReturn(Single.just(emptyList()))
        Assert.assertEquals(dataSource.loadFunction(-1, 1), emptyList<String>())
        verify(categoryClient, never()).resetSubCategoryContinuation("test")
    }
}
