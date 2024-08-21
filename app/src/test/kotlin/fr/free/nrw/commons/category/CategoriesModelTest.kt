package fr.free.nrw.commons.category

import categoryItem
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import media
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

    @Mock
    internal lateinit var gpsCategoryModel: GpsCategoryModel

    private lateinit var categoriesModel: CategoriesModel

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        categoriesModel = CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
    }

    // Test Case for verifying that Categories search (MW api calls)
    @Test
    fun searchAllFoundCaseTest() {
        val categoriesModel = CategoriesModel(categoryClient, mock(), mock())

        val expectedList = listOf(CategoryItem(
            "Test", "", "", false))
        whenever(
            categoryClient.searchCategoriesForPrefix(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt()
            )
        )
            .thenReturn(Single.just(expectedList))

        // Checking if both return "Test"
        val expectedItems = expectedList.map { CategoryItem(
            it.name, it.description, it.thumbnail, false) }
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
        val depictedItem = depictedItem(commonsCategories = listOf(CategoryItem(
            "depictionCategory", "", "", false)))

        whenever(gpsCategoryModel.categoriesFromLocation)
            .thenReturn(BehaviorSubject.createDefault(listOf(CategoryItem(
                "gpsCategory", "", "", false))))
        whenever(
            categoryClient.searchCategories(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt()
            )
        )
            .thenReturn(Single.just(listOf(CategoryItem(
                "titleSearch", "", "", false))))
        whenever(categoryDao.recentCategories(25)).thenReturn(listOf(CategoryItem(
            "recentCategories","","", false)))
        whenever(categoryClient.getCategoriesByName("depictionCategory",
            "depictionCategory", 25)).thenReturn(Single.just(listOf(CategoryItem(
            "commonsCategories","","", false))))
        val imageTitleList = listOf("Test")
        CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
            .searchAll("", imageTitleList, listOf(depictedItem))
            .test()
            .assertValue(
                listOf(
                    categoryItem("commonsCategories"),
                    categoryItem("gpsCategory"),
                    categoryItem("titleSearch"),
                    categoryItem("recentCategories")
                )
            )
        imageTitleList.forEach {
            verify(categoryClient).searchCategories(it, CategoriesModel.SEARCH_CATS_LIMIT)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetCategoriesByName(){
        categoriesModel.getCategoriesByName(listOf("Test"))
    }

    @Test
    @Throws(Exception::class)
    fun `Test buildCategories when it returns non empty list`(){
        whenever(categoryClient.getCategoriesByName("Test",
            "Test", CategoriesModel.SEARCH_CATS_LIMIT
        )).thenReturn(Single.just(listOf(categoryItem())))
        categoriesModel.buildCategories("Test")
    }

    @Test
    @Throws(Exception::class)
    fun `Test buildCategories when it returns empty list`(){
        whenever(categoryClient.getCategoriesByName("Test",
            "Test", CategoriesModel.SEARCH_CATS_LIMIT
        )).thenReturn(Single.just(emptyList()))
        categoriesModel.buildCategories("Test")
    }

    @Test
    @Throws(Exception::class)
    fun testGetSelectedExistingCategories(){
        categoriesModel.getSelectedExistingCategories()
    }

    @Test
    @Throws(Exception::class)
    fun testSetSelectedExistingCategories(){
        categoriesModel.setSelectedExistingCategories(mutableListOf("Test"))
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is null and item is selected`(){
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "name",
                "des",
                "image",
                true
            ), null)
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is null and item is not selected`(){
        categoriesModel.onCategoryItemClicked(categoryItem(), null)
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is selected and media contains category`(){
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "categories",
                "des",
                "image",
                true
            ), media())
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is selected and media does not contains category`(){
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "name",
                "des",
                "image",
                true
            ), media())
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is not selected and media contains category`(){
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "categories",
                "des",
                "image",
                false
            ), media())
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is not selected and media does not contains category`(){
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "name",
                "des",
                "image",
                false
            ), media())
    }
}
