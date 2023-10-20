package fr.free.nrw.commons.category

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult

class CategoryClientTest {
    @Mock
    internal lateinit var categoryInterface: CategoryInterface

    @InjectMocks
    lateinit var categoryClient: CategoryClient

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun searchCategoriesFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.searchCategories(anyString(), anyInt(), anyInt()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.searchCategories("tes", 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
        categoryClient.searchCategories("tes", 10, 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun searchCategoriesNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.searchCategories(anyString(), anyInt(), anyInt()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.searchCategories("tes", 10)
            .test()
            .assertValues(emptyList())
        categoryClient.searchCategories("tes", 10, 10)
            .test()
            .assertValues(emptyList())
    }
    @Test
    fun searchCategoriesForPrefixFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.searchCategoriesForPrefix(anyString(), anyInt(), anyInt()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.searchCategoriesForPrefix("tes", 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
        categoryClient.searchCategoriesForPrefix("tes", 10, 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun searchCategoriesForPrefixNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.searchCategoriesForPrefix(anyString(), anyInt(), anyInt()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.searchCategoriesForPrefix("tes", 10)
            .test()
            .assertValues(emptyList())
        categoryClient.searchCategoriesForPrefix("tes", 10, 10)
            .test()
            .assertValues(emptyList())
    }

    @Test
    fun getCategoriesByNameFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.getCategoriesByName(anyString(), anyString(),
            anyInt(), anyInt()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.getCategoriesByName("tes", "tes", 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "",
                "", false)))
        categoryClient.getCategoriesByName("tes" , "tes",
            10, 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "",
                "", false)))
    }

    @Test
    fun getCategoriesByNameNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.getCategoriesByName(anyString(), anyString(),
            anyInt(), anyInt()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.getCategoriesByName("tes", "tes",
            10)
            .test()
            .assertValues(emptyList())
        categoryClient.getCategoriesByName("tes", "tes",
            10, 10)
            .test()
            .assertValues(emptyList())
    }

    @Test
    fun getParentCategoryListFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.getParentCategoryList(anyString(), anyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.getParentCategoryList("tes")
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun getParentCategoryListNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.getParentCategoryList(anyString(), anyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.getParentCategoryList("tes")
            .test()
            .assertValues(emptyList())
    }

    @Test
    fun getSubCategoryListFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.getSubCategoryList("tes", emptyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient.getSubCategoryList("tes")
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun getSubCategoryListNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.getSubCategoryList(
            anyString(),
            anyMap()
        ))
            .thenReturn(Single.just(mockResponse))
        categoryClient.getSubCategoryList("tes")
            .test()
            .assertValues(emptyList())
    }

    private fun withMockResponse(title: String): MwQueryResponse? {
        val mwQueryPage: MwQueryPage = mock()
        whenever(mwQueryPage.title()).thenReturn(title)
        val mwQueryResult: MwQueryResult = mock()
        whenever(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        whenever(mockResponse.query()).thenReturn(mwQueryResult)
        return mockResponse
    }

    private fun withNullPages(): MwQueryResponse? {
        val mwQueryResult = mock(MwQueryResult::class.java)
        whenever(mwQueryResult.pages()).thenReturn(null)
        val mockResponse = mock(MwQueryResponse::class.java)
        whenever(mockResponse.query()).thenReturn(mwQueryResult)
        return mockResponse
    }
}
