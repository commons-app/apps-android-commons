package fr.free.nrw.commons.category

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString

/** Verifies that caption based suggestions search a caption shared by several pictures once. */
class CategoriesModelTitleSearchTest {
    private val categoryClient = mock<CategoryClient>()
    private val categoryDao = mock<CategoryDao>()
    private val gpsCategoryModel = mock<GpsCategoryModel>()
    private lateinit var model: CategoriesModel

    @Before
    fun setUp() {
        whenever(gpsCategoryModel.categoriesFromLocation).thenReturn(BehaviorSubject.createDefault(emptyList()))
        whenever(categoryDao.recentCategories(CategoriesModel.SEARCH_CATS_LIMIT)).thenReturn(emptyList())
        whenever(categoryClient.searchCategories(anyString(), anyInt(), anyInt()))
            .thenReturn(Single.just(listOf(CategoryItem("Olive trees", "", "", false))))
        model = CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
    }

    @Test
    fun `the same caption on 20 pictures is searched once`() {
        model.searchAll("", List(20) { "Olive tree" }, emptyList()).test()

        verify(categoryClient, times(1)).searchCategories("Olive tree", CategoriesModel.SEARCH_CATS_LIMIT, 0)
    }

    @Test
    fun `different captions are each searched`() {
        model.searchAll("", listOf("Olive tree", "Villa Borghese", "Olive tree"), emptyList()).test()

        verify(categoryClient, times(1)).searchCategories("Olive tree", CategoriesModel.SEARCH_CATS_LIMIT, 0)
        verify(categoryClient, times(1)).searchCategories("Villa Borghese", CategoriesModel.SEARCH_CATS_LIMIT, 0)
    }
}
