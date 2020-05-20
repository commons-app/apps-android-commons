package fr.free.nrw.commons.category

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
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
        val categoriesModel = CategoriesModel(categoryClient, mock(), mock())

        val expectedList = listOf("Test")
        whenever(categoryClient.searchCategoriesForPrefix("tes", 25))
            .thenReturn(Observable.just(expectedList))

        // Checking if both return "Test"
        val expectedItems = expectedList.map { CategoryItem(it, false) }
        categoriesModel.searchAll("tes", emptyList())
            .test()
            .assertValues(expectedItems)

        categoriesModel.searchAll("Tes", emptyList())
            .test()
            .assertValues(expectedItems)
    }

    @Test
    fun `searchAll with empty search terms creates results from gps, title search & recents`() {
        val gpsCategoryModel: GpsCategoryModel = mock()

        whenever(gpsCategoryModel.categoriesFromLocation)
            .thenReturn(BehaviorSubject.createDefault(listOf("gpsCategory")))
        whenever(categoryClient.searchCategories("tes", 25))
            .thenReturn(Observable.just(listOf("titleSearch")))
        whenever(categoryDao.recentCategories(25)).thenReturn(listOf("recentCategories"))
        CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
            .searchAll("", listOf("tes"))
            .test()
            .assertValue(
                listOf(
                    CategoryItem("gpsCategory", false),
                    CategoryItem("titleSearch", false),
                    CategoryItem("recentCategories", false)
                )
            )
    }
}
