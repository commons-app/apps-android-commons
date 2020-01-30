package fr.free.nrw.commons.category

import io.reactivex.Observable
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult

//class for testing CategoriesModel class
class CategoriesModelTest {
    @Mock
    internal var categoryInterface: CategoryInterface? = null

    @Mock
    internal var categoryItem: CategoryItem? = null

    @InjectMocks
    var categoryClient: CategoryClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    // Test Case for verifying that Categories search (MW api calls) are case-insensitive
    @Test
    fun searchAllFoundCaseTest() {
       val mwQueryPage = Mockito.mock(MwQueryPage::class.java)
        Mockito.`when`(mwQueryPage.title()).thenReturn("Category:Test")
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)
        val categoriesModel: CategoriesModel = CategoriesModel(categoryClient,null,null)

        Mockito.`when`(categoryInterface!!.searchCategoriesForPrefix(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(Observable.just(mockResponse))

        // Checking if both return "Test"
        val actualCategoryName = categoriesModel!!.searchAll("tes",null).blockingFirst()
        assertEquals("Test", actualCategoryName.getName())
        
        val actualCategoryNameCaps = categoriesModel!!.searchAll("Tes",null).blockingFirst()
        assertEquals("Test", actualCategoryNameCaps.getName())
    }
}