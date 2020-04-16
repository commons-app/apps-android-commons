package fr.free.nrw.commons.category

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Observable
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.MockitoAnnotations

//class for testing CategoriesModel class
class CategoriesModelTest {

    @Mock
    internal lateinit var categoryDao: CategoryDao

    @Mock
    internal lateinit var categoryClient: CategoryClient

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    // Test Case for verifying that Categories search (MW api calls) are case-insensitive
    @Test
    fun searchAllFoundCaseTest() {
        val categoriesModel = CategoriesModel(categoryClient, null, null, mock())

        whenever(categoryClient.searchCategoriesForPrefix(anyString(), eq(25)))
            .thenReturn(Observable.just("Test"))

        // Checking if both return "Test"
        val actualCategoryName = categoriesModel.searchAll("tes", null).blockingFirst()
        assertEquals("Test", actualCategoryName.name)

        val actualCategoryNameCaps = categoriesModel.searchAll("Tes", null).blockingFirst()
        assertEquals("Test", actualCategoryNameCaps.name)
    }

    /**
     * For testing the substring search algorithm for Categories search
     * To be more precise it tests the In Between substring( ex: searching `atte`
     * will give search suggestions: `Latte`, `Iced latte` e.t.c) which has been described
     * on github repo wiki:
     * https://github.com/commons-app/apps-android-commons/wiki/Category-suggestions-(readme)#user-content-3-category-search-when-typing-in-the-search-field-has-been-made-more-flexible
     */
    @Test
    fun searchAllFoundCaseTestForSubstringSearch() {
        val gpsCategoryModel: GpsCategoryModel = mock()
        val kvStore: JsonKvStore = mock()

        whenever(gpsCategoryModel.categoryList).thenReturn(listOf("gpsCategory"))
        whenever(categoryClient.searchCategories("tes", 25))
            .thenReturn(Observable.just("tes"))
        whenever(kvStore.getString("Category", "")).thenReturn("Random Value")
        whenever(categoryDao.recentCategories(25)).thenReturn(listOf("recentCategories"))
        CategoriesModel(
            categoryClient,
            categoryDao,
            kvStore,
            gpsCategoryModel
        ).searchAll(null, listOf("tes"))
            .test()
            .assertValues(
                CategoryItem("gpsCategory", false),
                CategoryItem("tes", false),
                CategoryItem("Random Value", false),
                CategoryItem("recentCategories", false)
            )
    }
}
