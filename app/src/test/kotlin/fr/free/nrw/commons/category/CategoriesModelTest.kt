package fr.free.nrw.commons.category

import categoryItem
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
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

    // Test Case for verifying that Categories search (MW api calls)
    @Test
    fun searchAllFoundCaseTest() {
        val categoriesModel = CategoriesModel(categoryClient, mock(), mock())

        val expectedList = listOf("Test")
        whenever(
            categoryClient.searchCategoriesForPrefix(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt()
            )
        )
            .thenReturn(Single.just(expectedList))

        // Checking if both return "Test"
        val expectedItems = expectedList.map { CategoryItem(it, false) }
        var categoryTerm = "Test"
        categoriesModel.searchAll(categoryTerm, emptyList(), emptyList())
            .test()
            .assertValues(expectedItems)

        verify(categoryClient).searchCategoriesForPrefix(
            categoryTerm,
            CategoriesModel.SEARCH_CATS_LIMIT
        )

        categoriesModel.searchAll(categoryTerm, emptyList(), emptyList())
            .test()
            .assertValues(expectedItems)
    }


    @Test
    fun `searchAll with empty search terms creates results from gps, title search & recents`() {
        val gpsCategoryModel: GpsCategoryModel = mock()
        val depictedItem = depictedItem(commonsCategories = listOf("depictionCategory"))

        whenever(gpsCategoryModel.categoriesFromLocation)
            .thenReturn(BehaviorSubject.createDefault(listOf("gpsCategory")))
        whenever(categoryClient.searchCategories("tes", 25))
            .thenReturn(Single.just(listOf("titleSearch")))
        whenever(categoryDao.recentCategories(25)).thenReturn(listOf("recentCategories"))
        val imageTitleList = listOf("Test")
        CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
            .searchAll("", imageTitleList, listOf(depictedItem))
            .test()
            .assertValue(
                listOf(
                    categoryItem("depictionCategory"),
                    categoryItem("gpsCategory"),
                    categoryItem("titleSearch"),
                    categoryItem("recentCategories")
                )
            )
        imageTitleList.forEach {
            verify(categoryClient).searchCategories(it, CategoriesModel.SEARCH_CATS_LIMIT)
        }
    }
}
