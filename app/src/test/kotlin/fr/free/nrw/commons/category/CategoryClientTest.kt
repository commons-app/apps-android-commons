package fr.free.nrw.commons.category

import categoryItem
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

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
        categoryClient
            .searchCategories("tes", 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
        categoryClient
            .searchCategories("tes", 10, 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun searchCategoriesNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.searchCategories(anyString(), anyInt(), anyInt()))
            .thenReturn(Single.just(mockResponse))
        categoryClient
            .searchCategories("tes", 10)
            .test()
            .assertValues(emptyList())
        categoryClient
            .searchCategories("tes", 10, 10)
            .test()
            .assertValues(emptyList())
    }

    @Test
    fun searchCategoriesForPrefixFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.searchCategoriesForPrefix(anyString(), anyInt(), anyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient
            .searchCategoriesForPrefix("tes", 10)
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun searchCategoriesForPrefixNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.searchCategoriesForPrefix(anyString(), anyInt(), anyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient
            .searchCategoriesForPrefix("tes", 10)
            .test()
            .assertValues(emptyList())
    }

    @Test
    fun `hidden categories from the API are dropped`() {
        val response =
            GsonUtil.defaultGson.fromJson(
                """{"query":{"pages":[
                    {"title":"Category:CC-BY-SA-4.0","categoryinfo":{"hidden":true}},
                    {"title":"Category:Supermarkets in Tokyo","categoryinfo":{}}
                ]}}""",
                MwQueryResponse::class.java,
            )
        whenever(categoryInterface.searchCategoriesWithInfo(anyString(), anyInt(), anyMap()))
            .thenReturn(Single.just(response))

        val names =
            categoryClient
                .nextTextSearchPage("supermarkets in tokyo", 10)
                .blockingGet()
                .map { it.name }

        assertEquals(listOf("Supermarkets in Tokyo"), names)
    }

    @Test
    fun `prefix search asks for the next page with the continuation of the previous one`() {
        val continuation = mapOf("gapcontinue" to "Cats_B", "continue" to "gapcontinue||")
        val firstPage = withMockResponse("Category:Cats A")
        whenever(firstPage!!.continuation()).thenReturn(continuation)
        val lastPage = withMockResponse("Category:Cats B")
        whenever(lastPage!!.continuation()).thenReturn(null)
        whenever(categoryInterface.searchCategoriesForPrefix("Cats", 10, emptyMap()))
            .thenReturn(Single.just(firstPage))
        whenever(categoryInterface.searchCategoriesForPrefix("Cats", 10, continuation))
            .thenReturn(Single.just(lastPage))

        categoryClient.searchCategoriesForPrefix("Cats", 10).test().assertValues(listOf(categoryItem("Cats A")))
        assertTrue(categoryClient.hasMorePrefixPages("Cats"))
        categoryClient.nextPrefixPage("Cats", 10).test().assertValues(listOf(categoryItem("Cats B")))
        assertFalse(categoryClient.hasMorePrefixPages("Cats"))
        categoryClient.nextPrefixPage("Cats", 10).test().assertValues(emptyList())

        // A new search starts from the first page again
        categoryClient.searchCategoriesForPrefix("Cats", 10).test().assertValues(listOf(categoryItem("Cats A")))
    }

    @Test
    fun getCategoriesByNameFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(
            categoryInterface.getCategoriesByName(
                anyString(),
                anyString(),
                anyInt(),
                anyInt(),
            ),
        ).thenReturn(Single.just(mockResponse))
        categoryClient
            .getCategoriesByName("tes", "tes", 10)
            .test()
            .assertValues(
                listOf(
                    CategoryItem(
                        "Test",
                        "",
                        "",
                        false,
                    ),
                ),
            )
        categoryClient
            .getCategoriesByName(
                "tes",
                "tes",
                10,
                10,
            ).test()
            .assertValues(
                listOf(
                    CategoryItem(
                        "Test",
                        "",
                        "",
                        false,
                    ),
                ),
            )
    }

    @Test
    fun getCategoriesByTitlesFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(
            categoryInterface.getCategoriesByTitles(
                anyString(),
                anyInt(),
            ),
        ).thenReturn(Single.just(mockResponse))
        categoryClient
            .getCategoriesOfImage("tes", 10)
            .test()
            .assertValues(
                listOf(
                    CategoryItem(
                        "Test",
                        "",
                        "",
                        false,
                    ),
                ),
            )
        categoryClient
            .getCategoriesOfImage(
                "tes",
                10,
            ).test()
            .assertValues(
                listOf(
                    CategoryItem(
                        "Test",
                        "",
                        "",
                        false,
                    ),
                ),
            )
    }

    @Test
    fun getCategoriesByNameNull() {
        val mockResponse = withNullPages()
        whenever(
            categoryInterface.getCategoriesByName(
                anyString(),
                anyString(),
                anyInt(),
                anyInt(),
            ),
        ).thenReturn(Single.just(mockResponse))
        categoryClient
            .getCategoriesByName(
                "tes",
                "tes",
                10,
            ).test()
            .assertValues(emptyList())
        categoryClient
            .getCategoriesByName(
                "tes",
                "tes",
                10,
                10,
            ).test()
            .assertValues(emptyList())
    }

    @Test
    fun getCategoriesByTitlesNull() {
        val mockResponse = withNullPages()
        whenever(
            categoryInterface.getCategoriesByTitles(
                anyString(),
                anyInt(),
            ),
        ).thenReturn(Single.just(mockResponse))
        categoryClient
            .getCategoriesOfImage(
                "tes",
                10,
            ).test()
            .assertValues(emptyList())
        categoryClient
            .getCategoriesOfImage(
                "tes",
                10,
            ).test()
            .assertValues(emptyList())
    }

    @Test
    fun getParentCategoryListFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.getParentCategoryList(anyString(), anyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient
            .getParentCategoryList("tes")
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun getParentCategoryListNull() {
        val mockResponse = withNullPages()
        whenever(categoryInterface.getParentCategoryList(anyString(), anyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient
            .getParentCategoryList("tes")
            .test()
            .assertValues(emptyList())
    }

    @Test
    fun getSubCategoryListFound() {
        val mockResponse = withMockResponse("Category:Test")
        whenever(categoryInterface.getSubCategoryList("tes", emptyMap()))
            .thenReturn(Single.just(mockResponse))
        categoryClient
            .getSubCategoryList("tes")
            .test()
            .assertValues(listOf(CategoryItem("Test", "", "", false)))
    }

    @Test
    fun getSubCategoryListNull() {
        val mockResponse = withNullPages()
        whenever(
            categoryInterface.getSubCategoryList(
                anyString(),
                anyMap(),
            ),
        ).thenReturn(Single.just(mockResponse))
        categoryClient
            .getSubCategoryList("tes")
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
